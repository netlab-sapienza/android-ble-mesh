package it.drone.mesh.models;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

public class UserList {

    private static final String TAG = UserList.class.getSimpleName();
    private static ArrayList<User> users = new ArrayList<>();


    public static User getUser(int index) throws NoSuchElementException {
        User u = users.get(index);
        if (u == null)
            throw new NoSuchElementException("User non presente all'interno della lista");
        else
            return u;
    }

    public static User getUser(String name) throws NoSuchElementException {
        for (User i : users) {
            if (i.getUserName().equals(name))
                return i;
        }
        throw new NoSuchElementException("User non presente all'interno della lista");
    }

    public static void addUser(User user) {
        users.add(user);
        Log.i(TAG, "added User: " + user.getUserName());
    }

    public static List<User> getUserList() {
        return users;
    }

    public static void cleanUserList() {
        Log.d(TAG, "OUD:" + "Lista pulita");
        users.clear();
    }

    public static User removeUser(String name) {
        for (User i : users) {
            if (i.getUserName().equals(name)) {
                users.remove(i);
                Log.i(TAG, "removed User: " + i.getUserName());
                return i;
            }
        }

        Log.e(TAG, "removeUser: User not found, Lista: " + printList());

        return null;
    }

    public static String printList() {
        StringBuilder res = new StringBuilder();
        for (User i : users)
            res.append("Username: ").append(i.getUserName()).append("ID mistico da decidere : none\n");

        return res.toString();
    }

}