package it.drone.mesh;

import java.util.LinkedList;

import it.drone.mesh.models.User;

public class UserList {
    static LinkedList<User> userlist = new LinkedList<>();

    public static User getUser(String name) {
        for (User temp : userlist)
            if (temp.getUserName().equals(name))
                return temp;

        return null;
    }

    public static void addUser(User user) {
        userlist.add(user);
    }

}