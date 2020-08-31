/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.github.oasis.core.services.api.controllers.admin;

import io.github.oasis.core.model.TeamObject;
import io.github.oasis.core.model.UserObject;
import io.github.oasis.core.services.api.controllers.AbstractController;
import io.github.oasis.core.services.api.services.UserTeamService;
import io.github.oasis.core.services.api.to.UserGameAssociationRequest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Isuru Weerarathna
 */
@RestController
@RequestMapping(
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
)
public class UserController extends AbstractController {

    private final UserTeamService userTeamService;

    public UserController(UserTeamService userTeamService) {
        this.userTeamService = userTeamService;
    }

    @PostMapping("/admin/users")
    public UserObject registerUser(@RequestBody UserObject user) {
        return userTeamService.addUser(user);
    }

    void readUserProfile() {}
    void browseUsers() {}
    void updateUser() {}
    void deactivateUser() {}

    @PostMapping("/admin/teams")
    public TeamObject addTeam(@RequestBody TeamObject team) {
        return userTeamService.addTeam(team);
    }

    void updateTeam() {}

    @PostMapping("/admin/users/{userId}/teams")
    void addUserToTeam(@PathVariable("userId") Integer userId,
                       @RequestBody UserGameAssociationRequest request) {
        userTeamService.addUserToTeam(userId, request.getGameId(), request.getTeamId());
    }

    void addUsersToTeam() {}
}
