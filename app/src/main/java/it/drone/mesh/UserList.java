package it.drone.mesh;

import android.util.Log;

import java.util.HashMap;

import it.drone.mesh.models.User;

public class UserList {

    private static final String TAG = UserList.class.getSimpleName();
    private static HashMap<String, User> users = new HashMap<>();


    public static User getUser(String name) {
        return users.get(name);
    }

    public static void addUser(User user) {
        users.put(user.getUserName(), user);
        Log.i(TAG, "added User: " + user.getUserName());
    }

    public static void removeUser(String name) {
        User u = users.remove(name);
        Log.i(TAG, "removed User: " + u.getUserName());
    }

    public static void listStatus() {
        for (String key : users.keySet()) {
            Log.i(TAG, "listStatus: chiave =" + key + " , valore = " + users.get(key));
        }
    }
}