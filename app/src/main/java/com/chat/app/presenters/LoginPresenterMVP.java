package com.chat.app.presenters;


import com.chat.app.model.User;

public class LoginPresenterMVP {
    private User user;
    private View view;

    public LoginPresenterMVP(View view) {
        this.user = new User();
        this.view = view;
    }

    public void performLoginProcess(User foundUser){
        user = foundUser;
        view.logsIn(user);

    }

    public void updateEmail(String email){
        user.setEmail(email);
        view.logsIn(user);

    }

    public interface View{

        void logsIn(User user);
        void showProgressBar();
        void hideProgressBar();

    }
}


