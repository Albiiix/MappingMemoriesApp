package com.example.mappingmemoriesapp.Models;

import android.app.Application;

import com.example.mappingmemoriesapp.Models.User;

public class UserClient extends Application {

    //Clase para establecer el usuario actual

    private User user = null;

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

}