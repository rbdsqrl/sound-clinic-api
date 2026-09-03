package com.simplehearing.task.controller;

import com.simplehearing.auth.security.UserPrincipal;
import com.simplehearing.common.dto.ApiResponse;
import com.simplehearing.common.dto.PagedResponse;
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
import com.simplehearing.task.entity.TaskLog;
import com.simplehearing.task.enums.TaskLogType;
import com.simplehearing.task.enums.TaskPriority;
import com.simplehearing.task.enums.TaskStatus;
import com.simplehearing.task.repository.TaskAssigneeRepository;
import com.simplehearing.task.repository.TaskAttachmentRepository;
import com.simplehearing.task.repository.TaskCommentRepository;
import com.simplehearing.task.repository.TaskLogRepository;
import com.simplehearing.task.repository.TaskRepository;
import com.simplehearing.storage.StorageService;
import com.simplehearing.user.entity.User;
import com.simplehearing.user.enums.Role;
import com.simplehearing.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
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
    private final TaskLogRepository logRepository;
    private final UserRepository userRepository;
    private final StorageService storageService;
    private final EmailService emailService;
    private final OrganisationRepository organisationRepository;

    public TaskController(TaskRepository taskRepository,
                          TaskAssigneeRepository assigneeRepository,
                          TaskCommentRepository commentRepository,
                          TaskAttachmentRepository attachmentRepository,
                          TaskLogRepository logRepository,
                          UserRepository userRepository,
                          StorageService storageService,
                          EmailService emailService,
                          OrganisationRepository organisationRepository) {
        this.taskRepository        = taskRepository;
        this.assigneeRepository    = assigneeRepository;
        this.commentRepository     = commentRepository;
        this.attachmentRepository  = attachmentRepository;
        this.logRepository         = logRepository;
        this.userRepository        = userRepository;
        this.storageService        = storageService;
        this.emailService          = emailService;
        this.organisationRepository = organisationRepository;
    }

    // ── List tasks ─────────────────────────────────────────────────────────────

    @Operation(
        summary = "List tasks, paginated — admins see all, others see the ones they were assigned or raised",
        description = "Defaults to 20 per page, sorted by createdAt descending. Pass `mine=true` and/or " +
                      "`status` (comma-separated) to scope to tasks assigned to the caller in specific " +
                      "statuses — used by the Dashboard's \"My Tasks\" widget; omitted, the endpoint behaves " +
                      "exactly as before (board view, scoped by role)."
    )
    @GetMapping
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD', 'THERAPIST', 'OFFICE_ADMIN')")
    public ResponseEntity<ApiResponse<PagedResponse<TaskResponse>>> list(
            @RequestParam(defaultValue = "false") boolean mine,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal UserPrincipal principal) {

        Page<Task> page;
        if (mine || status != null) {
            Set<TaskStatus> statuses = status == null
                    ? Set.of()
                    : Arrays.stream(status.split(","))
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .map(String::toUpperCase)
                            .map(TaskStatus::valueOf)
                            .collect(Collectors.toSet());
            boolean anyStatus = statuses.isEmpty();
            page = taskRepository.search(principal.getOrgId(), mine, principal.getId(), anyStatus, statuses, pageable);
        } else {
            Role role = principal.getUser().getRole();
            page = isManager(role)
                    ? taskRepository.findByOrgIdOrderByCreatedAtDesc(principal.getOrgId(), pageable)
                    : taskRepository.findByOrgIdAndAssigneeOrCreator(principal.getOrgId(), principal.getId(), pageable);
        }

        List<TaskResponse> content = enrich(page.getContent());
        PagedResponse<TaskResponse> result = new PagedResponse<>(
                content, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // ── Create task ────────────────────────────────────────────────────────────

    @Operation(summary = "Create a task and assign it to one or more staff members",
               description = "Open to every staff member — anyone can raise work and assign it to a colleague.")
    @PostMapping
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD', 'THERAPIST', 'OFFICE_ADMIN')")
    public ResponseEntity<ApiResponse<TaskResponse>> create(
            @Valid @RequestBody CreateTaskRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {

        List<User> assigneeUsers = loadAssignees(req.assignedTo(), principal);

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
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD', 'THERAPIST', 'OFFICE_ADMIN')")
    public ResponseEntity<ApiResponse<TaskResponse>> get(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {

        Task task = findAccessible(id, principal);
        return ResponseEntity.ok(ApiResponse.success(enrich(List.of(task)).get(0)));
    }

    // ── Update task ────────────────────────────────────────────────────────────

    @Operation(summary = "Update task details",
               description = "Managers may edit any task; everyone else only the tasks they raised.")
    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD', 'THERAPIST', 'OFFICE_ADMIN')")
    public ResponseEntity<ApiResponse<TaskResponse>> update(
            @PathVariable UUID id,
            @RequestBody UpdateTaskRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {

        Task task = findManageable(id, principal);

        String oldTitle       = task.getTitle();
        String oldDescription = task.getDescription();
        TaskPriority oldPriority = task.getPriority();

        if (req.title()       != null) task.setTitle(req.title());
        if (req.description() != null) task.setDescription(req.description());
        if (req.dueDate()     != null) task.setDueDate(req.dueDate());
        if (req.priority()    != null) task.setPriority(req.priority());

        List<User> newAssigneeUsers = List.of();
        if (req.assignedTo() != null && !req.assignedTo().isEmpty()) {
            newAssigneeUsers = loadAssignees(req.assignedTo(), principal);

            assigneeRepository.deleteById_TaskId(task.getId());
            List<TaskAssignee> newAssignees = req.assignedTo().stream()
                    .map(uid -> new TaskAssignee(task.getId(), uid))
                    .toList();
            assigneeRepository.saveAll(newAssignees);

            User assigner = principal.getUser();
            String assignerName = assigner.getFirstName() + " " + assigner.getLastName();
            String orgName = organisationRepository.findById(principal.getOrgId())
                    .map(Organisation::getName).orElse("SimpleHearing");
            String dueDateStr = task.getDueDate() != null ? task.getDueDate().toString() : null;
            String pri = task.getPriority() != null ? task.getPriority().name() : "NORMAL";

            final List<User> finalAssigneeUsers = newAssigneeUsers;
            finalAssigneeUsers.forEach(u -> emailService.sendTaskAssignmentEmail(
                    u.getEmail(),
                    u.getFirstName() + " " + u.getLastName(),
                    assignerName,
                    task.getTitle(),
                    task.getDescription(),
                    dueDateStr,
                    pri,
                    orgName));
        }

        Task saved = taskRepository.save(task);

        User actor = principal.getUser();
        String actorName = actor.getFirstName() + " " + actor.getLastName();

        if (req.title() != null && !req.title().equals(oldTitle)) {
            logRepository.save(makeLog(saved.getOrgId(), saved.getId(),
                    TaskLogType.NAME_CHANGED, principal.getId(), actorName,
                    "renamed to \"" + req.title() + "\""));
        }
        if (req.description() != null && !req.description().equals(oldDescription)) {
            logRepository.save(makeLog(saved.getOrgId(), saved.getId(),
                    TaskLogType.DESCRIPTION_CHANGED, principal.getId(), actorName,
                    "updated description"));
        }
        if (req.priority() != null && req.priority() != oldPriority) {
            logRepository.save(makeLog(saved.getOrgId(), saved.getId(),
                    TaskLogType.PRIORITY_CHANGED, principal.getId(), actorName,
                    "changed priority from " + label(oldPriority) + " to " + label(req.priority())));
        }
        if (!newAssigneeUsers.isEmpty()) {
            String names = newAssigneeUsers.stream()
                    .map(u -> u.getFirstName() + " " + u.getLastName())
                    .collect(Collectors.joining(", "));
            logRepository.save(makeLog(saved.getOrgId(), saved.getId(),
                    TaskLogType.ASSIGNEE_CHANGED, principal.getId(), actorName,
                    "reassigned to " + names));
        }

        return ResponseEntity.ok(ApiResponse.success(enrich(List.of(saved)).get(0)));
    }

    // ── Update task status ─────────────────────────────────────────────────────

    @Operation(summary = "Update task status")
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD', 'THERAPIST', 'OFFICE_ADMIN')")
    public ResponseEntity<ApiResponse<TaskResponse>> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTaskStatusRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {

        Task task = findAccessible(id, principal);
        TaskStatus oldStatus = task.getStatus();

        task.setStatus(req.status());
        if (req.status() == TaskStatus.COMPLETED) {
            task.setCompletedAt(Instant.now());
        } else if (oldStatus == TaskStatus.COMPLETED) {
            task.setCompletedAt(null);
        }

        Task saved = taskRepository.save(task);

        User actor = principal.getUser();
        String actorName = actor.getFirstName() + " " + actor.getLastName();
        logRepository.save(makeLog(saved.getOrgId(), saved.getId(),
                TaskLogType.STATUS_CHANGED, principal.getId(), actorName,
                "changed status from " + label(oldStatus) + " to " + label(req.status())));

        return ResponseEntity.ok(ApiResponse.success(enrich(List.of(saved)).get(0)));
    }

    // ── Delete task ────────────────────────────────────────────────────────────

    @Operation(summary = "Delete a task",
               description = "Managers may delete any task; everyone else only the tasks they raised.")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD', 'THERAPIST', 'OFFICE_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {

        Task task = findManageable(id, principal);
        attachmentRepository.findByTaskIdOrderByCreatedAtAsc(task.getId())
                .forEach(a -> storageService.delete(a.getFileUrl()));
        taskRepository.delete(task);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // ── List task logs ─────────────────────────────────────────────────────────

    @Operation(summary = "List activity log for a task")
    @GetMapping("/{id}/logs")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD', 'THERAPIST', 'OFFICE_ADMIN')")
    public ResponseEntity<ApiResponse<List<TaskLogResponse>>> listLogs(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {

        Task task = findAccessible(id, principal);
        List<TaskLog> logs = logRepository.findByTaskIdOrderByCreatedAtAsc(task.getId());
        return ResponseEntity.ok(ApiResponse.success(
                logs.stream().map(TaskLogResponse::from).toList()));
    }

    // ── List comments ──────────────────────────────────────────────────────────

    @Operation(summary = "List all comments for a task")
    @GetMapping("/{id}/comments")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD', 'THERAPIST', 'OFFICE_ADMIN')")
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
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD', 'THERAPIST', 'OFFICE_ADMIN')")
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
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD', 'THERAPIST', 'OFFICE_ADMIN')")
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
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD', 'THERAPIST', 'OFFICE_ADMIN')")
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
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD', 'THERAPIST', 'OFFICE_ADMIN')")
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

        User actor = principal.getUser();
        logRepository.save(makeLog(task.getOrgId(), task.getId(),
                TaskLogType.ATTACHMENT_ADDED, principal.getId(),
                actor.getFirstName() + " " + actor.getLastName(),
                "added attachment \"" + saved.getFileName() + "\""));

        String presignedUrl = storageService.presign(saved.getFileUrl(), Duration.ofHours(1));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(TaskAttachmentResponse.from(
                        saved, actor.getFirstName(), actor.getLastName(), presignedUrl)));
    }

    // ── Delete attachment ──────────────────────────────────────────────────────

    @Operation(summary = "Delete a task attachment")
    @DeleteMapping("/{id}/attachments/{attachmentId}")
    @PreAuthorize("hasAnyRole('BUSINESS_OWNER', 'CLINIC_HEAD', 'THERAPIST', 'OFFICE_ADMIN')")
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

        String fileName = att.getFileName();
        storageService.delete(att.getFileUrl());
        attachmentRepository.delete(att);

        User actor = principal.getUser();
        logRepository.save(makeLog(att.getOrgId(), att.getTaskId(),
                TaskLogType.ATTACHMENT_DELETED, principal.getId(),
                actor.getFirstName() + " " + actor.getLastName(),
                "removed attachment \"" + fileName + "\""));

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

    /**
     * A task the caller may edit or delete: managers may touch any task in the org,
     * everyone else only the ones they raised themselves.
     */
    private Task findManageable(UUID id, UserPrincipal principal) {
        Task task = findOwned(id, principal);
        if (!isManager(principal.getUser().getRole())
                && !task.getAssignedBy().equals(principal.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "Only the person who created this task, or an admin, can change it");
        }
        return task;
    }

    /** Resolves assignee ids, rejecting anyone outside the caller's organisation. */
    private List<User> loadAssignees(List<UUID> assigneeIds, UserPrincipal principal) {
        List<User> users = userRepository.findAllById(assigneeIds);
        if (users.size() != assigneeIds.size()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "One or more assignees not found");
        }
        users.forEach(u -> {
            if (!u.getOrgId().equals(principal.getOrgId())) {
                throw new ApiException(HttpStatus.FORBIDDEN, "Assignee does not belong to this organisation");
            }
        });
        return users;
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
        return role == Role.BUSINESS_OWNER || role == Role.CLINIC_HEAD;
    }

    private TaskLog makeLog(UUID orgId, UUID taskId, TaskLogType type,
                            UUID actorId, String actorName, String details) {
        TaskLog log = new TaskLog();
        log.setOrgId(orgId);
        log.setTaskId(taskId);
        log.setLogType(type);
        log.setActorId(actorId);
        log.setActorName(actorName);
        log.setDetails(details);
        return log;
    }

    private static String label(TaskStatus s) {
        return s.name().replace('_', ' ').toLowerCase();
    }

    private static String label(TaskPriority p) {
        return p.name().charAt(0) + p.name().substring(1).toLowerCase();
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
                    t.getCreatedAt(), t.getUpdatedAt(),
                    t.getCompletedAt());
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
            String presignedUrl = storageService.presign(a.getFileUrl(), Duration.ofHours(1));
            return TaskAttachmentResponse.from(a,
                    uploader != null ? uploader.getFirstName() : "",
                    uploader != null ? uploader.getLastName()  : "",
                    presignedUrl);
        }).toList();
    }
}
