package com.learn.interviewmentor.controller;

import com.learn.interviewmentor.common.ApiResult;

import com.learn.interviewmentor.vo.material.StudyMaterialVo;
import com.learn.interviewmentor.model.StudyMaterial;
import com.learn.interviewmentor.model.User;
import com.learn.interviewmentor.security.CurrentUser;
import com.learn.interviewmentor.facade.StudyMaterialFacade;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Path;
import java.util.List;

/**
 * Study material as a student sees it.
 *
 * The list only ever contains what this caller is allowed - "sent to everyone",
 * "sent to me", and "unlocked by a plan I hold". That filtering happens in the
 * SQL, not on the client, and the download endpoint checks again rather than
 * assuming the caller only knows ids they were shown.
 */
@RestController
@RequestMapping("/api/materials")
@Tag(name = "6. Study material",
        description = "Notes, PDFs and links an admin has sent out. You see material addressed to "
                + "all students, addressed to you personally, or unlocked by a plan you hold - "
                + "nothing else.")
public class StudyMaterialController {

    private final StudyMaterialFacade materialFacade;

    public StudyMaterialController(StudyMaterialFacade materialFacade) {
        this.materialFacade = materialFacade;
    }

    @GetMapping
    @Operation(
            summary = "My study material",
            description = "Newest first. `kind` is FILE or LINK: a FILE is downloaded from "
                    + "`/api/materials/{id}/file`, a LINK is opened straight from `linkUrl`.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Material shared with me"),
            @ApiResponse(responseCode = "401", description = "Not logged in",
                    content = @Content(schema = @Schema(implementation = ApiResult.class)))
    })
    public ApiResult<List<StudyMaterialVo>> mine(@CurrentUser User student) {
        return materialFacade.visibleTo(student);
    }

    @GetMapping("/{id}/file")
    @Operation(
            summary = "Download a file",
            description = "Served as an attachment with `nosniff`, so an uploaded file can never "
                    + "render as a page in our own origin.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The file"),
            @ApiResponse(responseCode = "400", description = "That material is a link, not a file",
                    content = @Content(schema = @Schema(implementation = ApiResult.class))),
            @ApiResponse(responseCode = "403", description = "Not shared with you",
                    content = @Content(schema = @Schema(implementation = ApiResult.class))),
            @ApiResponse(responseCode = "404", description = "No such material, or the file is gone",
                    content = @Content(schema = @Schema(implementation = ApiResult.class)))
    })
    public ResponseEntity<Resource> download(@PathVariable Long id, @CurrentUser User caller) {
        StudyMaterial material = materialFacade.fileFor(id, caller);
        Path path = materialFacade.pathFor(material);

        String type = material.getContentType() == null
                ? "application/octet-stream" : material.getContentType();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(type))
                // The original name is given back as the download name. It was
                // stripped of quotes, backslashes and newlines on the way in, so
                // it cannot break out of the header here.
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + material.getOriginalName() + "\"")
                .header("X-Content-Type-Options", "nosniff")
                .body(new FileSystemResource(path));
    }
}
