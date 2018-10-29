package it.drone.mesh;

import android.util.Log;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

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

    public static List<User> getUserList() {
        List<User> res = new LinkedList<>();
        Set<String> temp = users.keySet();
        for (String name : temp) {
            res.add(getUser(name));
        }
        return res;
    }

    public static void clean() {
        Log.d(TAG, "OUD:" + "Lista pulita");
        users = new HashMap<>();
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