package com.chat.app.presenters;


import com.chat.app.model.User;

public class RegisterPresenterMVP {
    private User user;
    private View view;

    public RegisterPresenterMVP(View view) {
        this.user = new User();
        this.view = view;
    }

    public void performRegisterProcess(User foundUser){
        user = foundUser;
        view.registerUser(user);

    }

    public void updateEmail(String email){
        user.setEmail(email);
        view.registerUser(user);

    }

    public interface View{

        void registerUser(User user);
        void showProgressBar();
        void hideProgressBar();

    }
}


