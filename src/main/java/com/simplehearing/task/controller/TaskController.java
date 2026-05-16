package com.simplehearing.task.controller;

import com.simplehearing.auth.security.UserPrincipal;
import com.simplehearing.common.dto.ApiResponse;
import com.simplehearing.common.exception.ApiException;
import com.simplehearing.common.exception.ResourceNotFoundException;
import com.simplehearing.task.dto.*;
import com.simplehearing.task.entity.Task;
import com.simplehearing.task.entity.TaskAttachment;
import com.simplehearing.task.entity.TaskComment;
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
    private final TaskCommentRepository commentRepository;
    private final TaskAttachmentRepository attachmentRepository;
    private final UserRepository userRepository;
    private final StorageService storageService;

    public TaskController(TaskRepository taskRepository,
                          TaskCommentRepository commentRepository,
                          TaskAttachmentRepository attachmentRepository,
                          UserRepository userRepository,
                          StorageService storageService) {
        this.taskRepository      = taskRepository;
        this.commentRepository   = commentRepository;
        this.attachmentRepository = attachmentRepository;
        this.userRepository      = userRepository;
        this.storageService      = storageService;
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
                : taskRepository.findByOrgIdAndAssignedToOrderByCreatedAtDesc(
                        principal.getOrgId(), principal.getId());

        return ResponseEntity.ok(ApiResponse.success(enrich(tasks)));
    }

    // ── Create task ────────────────────────────────────────────────────────────

    @Operation(summary = "Create a task and assign it to a user")
    @PostMapping
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<TaskResponse>> create(
            @Valid @RequestBody CreateTaskRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {

        Task task = new Task();
        task.setOrgId(principal.getOrgId());
        task.setTitle(req.title());
        task.setDescription(req.description());
        task.setAssignedTo(req.assignedTo());
        task.setAssignedBy(principal.getId());
        task.setDueDate(req.dueDate());
        if (req.priority() != null) task.setPriority(req.priority());

        Task saved = taskRepository.save(task);
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

    // ── Update task (title / description / due date / priority / assignee) ────

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
        if (req.assignedTo()  != null) task.setAssignedTo(req.assignedTo());
        if (req.dueDate()     != null) task.setDueDate(req.dueDate());
        if (req.priority()    != null) task.setPriority(req.priority());

        Task saved = taskRepository.save(task);
        return ResponseEntity.ok(ApiResponse.success(enrich(List.of(saved)).get(0)));
    }

    // ── Update task status ─────────────────────────────────────────────────────

    @Operation(summary = "Update task status (OPEN / IN_PROGRESS / COMPLETED / CANCELLED)")
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
        if (!isManager(role)
                && !task.getAssignedTo().equals(principal.getId())
                && !task.getAssignedBy().equals(principal.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied");
        }
        return task;
    }

    private static boolean isManager(Role role) {
        return role == Role.BUSINESS_OWNER || role == Role.ADMIN;
    }

    private List<TaskResponse> enrich(List<Task> tasks) {
        if (tasks.isEmpty()) return List.of();

        Set<UUID> userIds = new HashSet<>();
        tasks.forEach(t -> { userIds.add(t.getAssignedTo()); userIds.add(t.getAssignedBy()); });

        Map<UUID, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        List<UUID> taskIds = tasks.stream().map(Task::getId).toList();

        Map<UUID, Long> commentCounts = commentRepository.countByTaskIdIn(taskIds).stream()
                .collect(Collectors.toMap(r -> r.getTaskId(), r -> r.getCnt()));
        Map<UUID, Long> attachmentCounts = attachmentRepository.countByTaskIdIn(taskIds).stream()
                .collect(Collectors.toMap(r -> r.getTaskId(), r -> r.getCnt()));

        return tasks.stream().map(t -> {
            User assignee = userMap.get(t.getAssignedTo());
            User assigner = userMap.get(t.getAssignedBy());
            return TaskResponse.from(t,
                    assignee != null ? assignee.getFirstName() : "",
                    assignee != null ? assignee.getLastName()  : "",
                    assigner != null ? assigner.getFirstName() : "",
                    assigner != null ? assigner.getLastName()  : "",
                    commentCounts.getOrDefault(t.getId(), 0L).intValue(),
                    attachmentCounts.getOrDefault(t.getId(), 0L).intValue());
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
