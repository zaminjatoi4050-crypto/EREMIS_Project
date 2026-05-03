package com.eremis.controller;

import com.eremis.model.User;
import com.eremis.service.UserService;

import java.util.List;

public class UserController {
    private final UserService userService = new UserService();
    public User createUser(User user)        { return userService.createUser(user); }
    public User updateUser(User user)        { return userService.updateUser(user); }
    public void changePassword(int userId, String newPassword) { userService.changePassword(userId, newPassword); }
    public void deleteUser(int id)           { userService.deleteUser(id); }
    public List<User> getAllUsers()           { return userService.getAllUsers(); }
}
