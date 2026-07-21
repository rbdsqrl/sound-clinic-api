package com.simplehearing.task.controller;

import com.simplehearing.auth.security.UserPrincipal;
import com.simplehearing.common.dto.ApiResponse;
import com.simplehearing.common.exception.ApiException;
import com.simplehearing.common.exception.ResourceNotFoundException;
import com.simplehearing.notification.EmailService;
import com.simplehearing.organisation.entity.Organisation;
import com.simplehearing.organisation.repository.OrganisationRepository;
import com.simplehearing.task.dto.*;
import com.simplehearing.task.entity.Task;
import com.simplehearing.task.entity.TaskAssignee;
import com.simplehearing.task.entity.TaskAttachment;
import com.simplehearing.task.entity.TaskComment;
import com.simplehearing.task.repository.TaskAssigneeRepository;
import com.simplehearing.task.repository.TaskAttachmentRepository;
import com.simplehearing.task.repository.TaskCommentRepository;
import com.simplehearing.task.repository.TaskRepository;
import com.simplehearing.storage.StorageService;
import com.simplehearing.user.entity.User;
import com.simplehearing.user.enums.Role;
import com.simplehearing.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Tag(name = "Tasks", description = "Task management")
@RestController
@RequestMapping("/api/v1/tasks")
public class TaskController {

    private final TaskRepository taskRepository;
    private final TaskAssigneeRepository assigneeRepository;
    private final TaskCommentRepository commentRepository;
    private final TaskAttachmentRepository attachmentRepository;
    private final UserRepository userRepository;
    private final StorageService storageService;
    private final EmailService emailService;
    private final OrganisationRepository organisationRepository;

    public TaskController(TaskRepository taskRepository,
                          TaskAssigneeRepository assigneeRepository,
                          TaskCommentRepository commentRepository,
                          TaskAttachmentRepository attachmentRepository,
                          UserRepository userRepository,
                          StorageService storageService,
                          EmailService emailService,
                          OrganisationRepository organisationRepository) {
        this.taskRepository        = taskRepository;
        this.assigneeRepository    = assigneeRepository;
        this.commentRepository     = commentRepository;
        this.attachmentRepository  = attachmentRepository;
        this.userRepository        = userRepository;
        this.storageService        = storageService;
        this.emailService          = emailService;
        this.organisationRepository = organisationRepository;
    }

    // ── List tasks ─────────────────────────────────────────────────────────────

    @Operation(summary = "List tasks — admins see all, others see only their own")
    @GetMapping
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'ADMIN', 'OFFICE_ADMIN', 'THERAPIST', 'DOCTOR')")
    public ResponseEntity<ApiResponse<List<TaskResponse>>> list(
            @AuthenticationPrincipal UserPrincipal principal) {

        Role role = principal.getUser().getRole();
        List<Task> tasks = isManager(role)
                ? taskRepository.findByOrgIdOrderByCreatedAtDesc(principal.getOrgId())
                : taskRepository.findByOrgIdAndAssignee(principal.getOrgId(), principal.getId());

        return ResponseEntity.ok(ApiResponse.success(enrich(tasks)));
    }

    // ── Create task ────────────────────────────────────────────────────────────

    @Operation(summary = "Create a task and assign it to one or more users")
    @PostMapping
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<TaskResponse>> create(
            @Valid @RequestBody CreateTaskRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {

        // Validate assignees belong to org
        List<User> assigneeUsers = userRepository.findAllById(req.assignedTo());
        if (assigneeUsers.size() != req.assignedTo().size()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "One or more assignees not found");
        }
        assigneeUsers.forEach(u -> {
            if (!u.getOrgId().equals(principal.getOrgId())) {
                throw new ApiException(HttpStatus.FORBIDDEN, "Assignee does not belong to this organisation");
            }
        });

        Task task = new Task();
        task.setOrgId(principal.getOrgId());
        task.setTitle(req.title());
        task.setDescription(req.description());
        task.setAssignedBy(principal.getId());
        task.setDueDate(req.dueDate());
        if (req.priority() != null) task.setPriority(req.priority());

        Task saved = taskRepository.save(task);

        List<TaskAssignee> assignees = req.assignedTo().stream()
                .map(uid -> new TaskAssignee(saved.getId(), uid))
                .toList();
        assigneeRepository.saveAll(assignees);

        User assigner = principal.getUser();
        String assignerName = assigner.getFirstName() + " " + assigner.getLastName();
        String orgName = organisationRepository.findById(principal.getOrgId())
                .map(Organisation::getName).orElse("SimpleHearing");
        String dueDateStr = saved.getDueDate() != null ? saved.getDueDate().toString() : null;
        String priority = saved.getPriority() != null ? saved.getPriority().name() : "NORMAL";

        assigneeUsers.forEach(u -> emailService.sendTaskAssignmentEmail(
                u.getEmail(),
                u.getFirstName() + " " + u.getLastName(),
                assignerName,
                saved.getTitle(),
                saved.getDescription(),
                dueDateStr,
                priority,
                orgName));

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(enrich(List.of(saved)).get(0)));
    }

    // ── Get task detail ────────────────────────────────────────────────────────

    @Operation(summary = "Get a single task")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'ADMIN', 'OFFICE_ADMIN', 'THERAPIST', 'DOCTOR')")
    public ResponseEntity<ApiResponse<TaskResponse>> get(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {

        Task task = findAccessible(id, principal);
        return ResponseEntity.ok(ApiResponse.success(enrich(List.of(task)).get(0)));
    }

    // ── Update task ────────────────────────────────────────────────────────────

    @Operation(summary = "Update task details")
    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<TaskResponse>> update(
            @PathVariable UUID id,
            @RequestBody UpdateTaskRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {

        Task task = findOwned(id, principal);
        if (req.title()       != null) task.setTitle(req.title());
        if (req.description() != null) task.setDescription(req.description());
        if (req.dueDate()     != null) task.setDueDate(req.dueDate());
        if (req.priority()    != null) task.setPriority(req.priority());

        if (req.assignedTo() != null && !req.assignedTo().isEmpty()) {
            assigneeRepository.deleteById_TaskId(task.getId());
            List<TaskAssignee> newAssignees = req.assignedTo().stream()
                    .map(uid -> new TaskAssignee(task.getId(), uid))
                    .toList();
            assigneeRepository.saveAll(newAssignees);

            List<User> newAssigneeUsers = userRepository.findAllById(req.assignedTo());
            User assigner = principal.getUser();
            String assignerName = assigner.getFirstName() + " " + assigner.getLastName();
            String orgName = organisationRepository.findById(principal.getOrgId())
                    .map(Organisation::getName).orElse("SimpleHearing");
            String dueDateStr = task.getDueDate() != null ? task.getDueDate().toString() : null;
            String priority = task.getPriority() != null ? task.getPriority().name() : "NORMAL";

            newAssigneeUsers.forEach(u -> emailService.sendTaskAssignmentEmail(
                    u.getEmail(),
                    u.getFirstName() + " " + u.getLastName(),
                    assignerName,
                    task.getTitle(),
                    task.getDescription(),
                    dueDateStr,
                    priority,
                    orgName));
        }

        Task saved = taskRepository.save(task);
        return ResponseEntity.ok(ApiResponse.success(enrich(List.of(saved)).get(0)));
    }

    // ── Update task status ─────────────────────────────────────────────────────

    @Operation(summary = "Update task status")
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'ADMIN', 'OFFICE_ADMIN', 'THERAPIST', 'DOCTOR')")
    public ResponseEntity<ApiResponse<TaskResponse>> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTaskStatusRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {

        Task task = findAccessible(id, principal);
        task.setStatus(req.status());
        Task saved = taskRepository.save(task);
        return ResponseEntity.ok(ApiResponse.success(enrich(List.of(saved)).get(0)));
    }

    // ── Delete task ────────────────────────────────────────────────────────────

    @Operation(summary = "Delete a task")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {

        Task task = findOwned(id, principal);
        attachmentRepository.findByTaskIdOrderByCreatedAtAsc(task.getId())
                .forEach(a -> storageService.delete(a.getFileUrl()));
        taskRepository.delete(task);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // ── List comments ──────────────────────────────────────────────────────────

    @Operation(summary = "List all comments for a task")
    @GetMapping("/{id}/comments")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'ADMIN', 'OFFICE_ADMIN', 'THERAPIST', 'DOCTOR')")
    public ResponseEntity<ApiResponse<List<TaskCommentResponse>>> listComments(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {

        Task task = findAccessible(id, principal);
        List<TaskComment> comments = commentRepository.findByTaskIdOrderByCreatedAtAsc(task.getId());
        return ResponseEntity.ok(ApiResponse.success(enrichComments(comments)));
    }

    // ── Add comment ────────────────────────────────────────────────────────────

    @Operation(summary = "Add a comment to a task")
    @PostMapping("/{id}/comments")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'ADMIN', 'OFFICE_ADMIN', 'THERAPIST', 'DOCTOR')")
    public ResponseEntity<ApiResponse<TaskCommentResponse>> addComment(
            @PathVariable UUID id,
            @Valid @RequestBody CreateCommentRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {

        Task task = findAccessible(id, principal);

        TaskComment comment = new TaskComment();
        comment.setOrgId(task.getOrgId());
        comment.setTaskId(task.getId());
        comment.setAuthorId(principal.getId());
        comment.setBody(req.body());

        TaskComment saved = commentRepository.save(comment);
        User author = principal.getUser();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(TaskCommentResponse.from(
                        saved, author.getFirstName(), author.getLastName())));
    }

    // ── Delete comment ─────────────────────────────────────────────────────────

    @Operation(summary = "Delete a comment")
    @DeleteMapping("/{id}/comments/{commentId}")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'ADMIN', 'OFFICE_ADMIN', 'THERAPIST', 'DOCTOR')")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @PathVariable UUID id,
            @PathVariable UUID commentId,
            @AuthenticationPrincipal UserPrincipal principal) {

        TaskComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));

        if (!comment.getTaskId().equals(id) || !comment.getOrgId().equals(principal.getOrgId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied");
        }

        Role role = principal.getUser().getRole();
        if (!isManager(role) && !comment.getAuthorId().equals(principal.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You can only delete your own comments");
        }

        commentRepository.delete(comment);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // ── List attachments ───────────────────────────────────────────────────────

    @Operation(summary = "List all attachments for a task")
    @GetMapping("/{id}/attachments")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'ADMIN', 'OFFICE_ADMIN', 'THERAPIST', 'DOCTOR')")
    public ResponseEntity<ApiResponse<List<TaskAttachmentResponse>>> listAttachments(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {

        Task task = findAccessible(id, principal);
        List<TaskAttachment> atts = attachmentRepository.findByTaskIdOrderByCreatedAtAsc(task.getId());
        return ResponseEntity.ok(ApiResponse.success(enrichAttachments(atts)));
    }

    // ── Upload attachment ──────────────────────────────────────────────────────

    @Operation(summary = "Upload a file attachment to a task")
    @PostMapping("/{id}/attachments")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'ADMIN', 'OFFICE_ADMIN', 'THERAPIST', 'DOCTOR')")
    public ResponseEntity<ApiResponse<TaskAttachmentResponse>> uploadAttachment(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal principal) throws IOException {

        Task task = findAccessible(id, principal);
        String url = storageService.store(file, "tasks/" + id);

        TaskAttachment att = new TaskAttachment();
        att.setOrgId(task.getOrgId());
        att.setTaskId(task.getId());
        att.setUploadedBy(principal.getId());
        att.setFileName(file.getOriginalFilename() != null ? file.getOriginalFilename() : "file");
        att.setFileUrl(url);
        att.setContentType(file.getContentType());
        att.setFileSizeBytes(file.getSize());

        TaskAttachment saved = attachmentRepository.save(att);
        User uploader = principal.getUser();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(TaskAttachmentResponse.from(
                        saved, uploader.getFirstName(), uploader.getLastName())));
    }

    // ── Delete attachment ──────────────────────────────────────────────────────

    @Operation(summary = "Delete a task attachment")
    @DeleteMapping("/{id}/attachments/{attachmentId}")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'ADMIN', 'OFFICE_ADMIN', 'THERAPIST', 'DOCTOR')")
    public ResponseEntity<ApiResponse<Void>> deleteAttachment(
            @PathVariable UUID id,
            @PathVariable UUID attachmentId,
            @AuthenticationPrincipal UserPrincipal principal) {

        TaskAttachment att = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment not found"));

        if (!att.getTaskId().equals(id) || !att.getOrgId().equals(principal.getOrgId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied");
        }

        Role role = principal.getUser().getRole();
        if (!isManager(role) && !att.getUploadedBy().equals(principal.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You can only delete your own attachments");
        }

        storageService.delete(att.getFileUrl());
        attachmentRepository.delete(att);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private Task findOwned(UUID id, UserPrincipal principal) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
        if (!task.getOrgId().equals(principal.getOrgId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied");
        }
        return task;
    }

    private Task findAccessible(UUID id, UserPrincipal principal) {
        Task task = findOwned(id, principal);
        Role role = principal.getUser().getRole();
        if (!isManager(role)) {
            boolean isAssignee = assigneeRepository.findById_TaskId(task.getId())
                    .stream().anyMatch(a -> a.getUserId().equals(principal.getId()));
            if (!isAssignee && !task.getAssignedBy().equals(principal.getId())) {
                throw new ApiException(HttpStatus.FORBIDDEN, "Access denied");
            }
        }
        return task;
    }

    private static boolean isManager(Role role) {
        return role == Role.BUSINESS_OWNER || role == Role.ADMIN;
    }

    private List<TaskResponse> enrich(List<Task> tasks) {
        if (tasks.isEmpty()) return List.of();

        List<UUID> taskIds = tasks.stream().map(Task::getId).toList();

        Map<UUID, List<TaskAssignee>> assigneesByTask = assigneeRepository.findByTaskIdIn(taskIds)
                .stream()
                .collect(Collectors.groupingBy(TaskAssignee::getTaskId));

        Set<UUID> userIds = new HashSet<>();
        tasks.forEach(t -> userIds.add(t.getAssignedBy()));
        assigneesByTask.values().forEach(list -> list.forEach(a -> userIds.add(a.getUserId())));

        Map<UUID, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        Map<UUID, Long> commentCounts = commentRepository.countByTaskIdIn(taskIds).stream()
                .collect(Collectors.toMap(r -> r.getTaskId(), r -> r.getCnt()));
        Map<UUID, Long> attachmentCounts = attachmentRepository.countByTaskIdIn(taskIds).stream()
                .collect(Collectors.toMap(r -> r.getTaskId(), r -> r.getCnt()));

        return tasks.stream().map(t -> {
            List<TaskResponse.AssigneeInfo> assignees = assigneesByTask
                    .getOrDefault(t.getId(), List.of())
                    .stream()
                    .map(a -> {
                        User u = userMap.get(a.getUserId());
                        return new TaskResponse.AssigneeInfo(
                                a.getUserId(),
                                u != null ? u.getFirstName() : "",
                                u != null ? u.getLastName()  : "");
                    })
                    .toList();

            User assigner = userMap.get(t.getAssignedBy());
            return new TaskResponse(
                    t.getId(), t.getOrgId(), t.getTitle(), t.getDescription(),
                    assignees,
                    t.getAssignedBy(),
                    assigner != null ? assigner.getFirstName() : "",
                    assigner != null ? assigner.getLastName()  : "",
                    t.getDueDate(), t.getPriority(), t.getStatus(),
                    commentCounts.getOrDefault(t.getId(), 0L).intValue(),
                    attachmentCounts.getOrDefault(t.getId(), 0L).intValue(),
                    t.getCreatedAt(), t.getUpdatedAt());
        }).toList();
    }

    private List<TaskCommentResponse> enrichComments(List<TaskComment> comments) {
        Set<UUID> authorIds = comments.stream().map(TaskComment::getAuthorId).collect(Collectors.toSet());
        Map<UUID, User> userMap = userRepository.findAllById(authorIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
        return comments.stream().map(c -> {
            User author = userMap.get(c.getAuthorId());
            return TaskCommentResponse.from(c,
                    author != null ? author.getFirstName() : "",
                    author != null ? author.getLastName()  : "");
        }).toList();
    }

    private List<TaskAttachmentResponse> enrichAttachments(List<TaskAttachment> atts) {
        Set<UUID> uploaderIds = atts.stream().map(TaskAttachment::getUploadedBy).collect(Collectors.toSet());
        Map<UUID, User> userMap = userRepository.findAllById(uploaderIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
        return atts.stream().map(a -> {
            User uploader = userMap.get(a.getUploadedBy());
            return TaskAttachmentResponse.from(a,
                    uploader != null ? uploader.getFirstName() : "",
                    uploader != null ? uploader.getLastName()  : "");
        }).toList();
    }
}
