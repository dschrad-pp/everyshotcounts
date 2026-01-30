package com.lektralabs.thrones.pallbearer.servlet;

import com.lektralabs.thrones.pallbearer.api.model.display.CurrentUser;
import com.lektralabs.thrones.pallbearer.jdbi.service.UserService;
import com.lektralabs.thrones.pallbearer.tus.TusUploadProcessor;
import com.lektralabs.thrones.pallbearer.tus.TusUploadServiceFactory;
import com.lektralabs.thrones.pallbearer.tus.TusUploadUtils;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class UploadServlet extends PatchServlet implements TusUploadUtils {
    private static final Logger logger = Logger.getLogger(UploadServlet.class);

    @Inject
    SecurityIdentity securityIdentity;

    @Inject
    UserService userService;

    @Inject
    TusUploadServiceFactory tusUploadServiceFactory;

    @Inject
    TusUploadProcessor tusUploadProcessor;

    private static final Set<String> VALID_ROLES = new HashSet<>(Arrays.asList("ADMIN", "COACH", "ATHLETE"));

    @Override
    protected void doOptions(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest("OPTIONS", request, response);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest("GET", request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest("POST", request, response);
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest("PUT", request, response);
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest("DELETE", request, response);
    }

    @Override
    protected void doHead(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest("HEAD", request, response);
    }

    @Override
    protected void doPatch(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest("PATCH", request, response);
    }

    protected void processRequest(String method, HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (isValidRole(securityIdentity.getRoles())) {
            // process drill and initial requests
            String drillItemId = getUuid(request.getRequestURI());
            CurrentUser currentUser = userService.getCurrentUser(securityIdentity.getPrincipal().getName());
            String ownerKey = getOwnerKey(drillItemId, currentUser.getId());
            tusUploadServiceFactory.getTusFileUploadService().process(request, response, ownerKey);

            // process finalizing of upload - if not complete, this will no-op
            response.addHeader("Access-Control-Expose-Headers","Location,Upload-Offset,Upload-Length");
            tusUploadProcessor.finalizeUpload(request.getRequestURI(), ownerKey, drillItemId, currentUser.getId());
        } else {
            logger.warn("unauthorized access to upload servlet");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "UNAUTHORIZED");
        }
    }

    private boolean isValidRole(Set<String> roles) {
        Set<String> intersection = new HashSet<>(roles);
        intersection.retainAll(VALID_ROLES);
        return intersection.size() > 0;
    }
}