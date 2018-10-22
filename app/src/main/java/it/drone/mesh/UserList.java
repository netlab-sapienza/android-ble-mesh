package it.drone.mesh;

import java.util.LinkedList;

import it.drone.mesh.models.User;

public class UserList {
    private static LinkedList<User> users = new LinkedList<>();

    public static User getUser(String name) {
        for (User temp : users)
            if (temp.getUserName().equals(name))
                return temp;

        return null;
    }

    public static void addUser(User user) {
        users.add(user);
    }

}