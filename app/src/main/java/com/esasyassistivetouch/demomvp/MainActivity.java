package com.esasyassistivetouch.demomvp;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.esasyassistivetouch.demomvp.ui.inter.IViewListener;
import com.esasyassistivetouch.demomvp.ui.presenter.PresenterLogin;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity implements IViewListener {

    @BindView(R.id.username)
    EditText username;

    @BindView(R.id.password)
    EditText password;

    private PresenterLogin mPresenterLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        this.mPresenterLogin = new PresenterLogin(this);

//        ExampleDialog exampleDialog = new ExampleDialog(this);
//        exampleDialog.setTitle(this.getString(R.string.app_name))
//                .setMessage(this.getString(R.string.action_sign_in))
//                .setNegativeAction(R.string.action_cancel, (dialog, which) -> exampleDialog.destroy())
//                .setPositiveAction(R.string.action_start, (dialog, which) -> {
//                })
//                .show();
        new MaterialAlertDialogBuilder(this).setTitle(R.string.app_name)
                .setCancelable(true)
                .setMessage(R.string.add_widget)
                .setNegativeButton("Cancel", (dialog, which) -> {

                })
                .setPositiveButton("Confirm", (dialog, which) -> {

                })
                .show();
    }

    @OnClick(R.id.username)
    public void onClickUsername() {

    }

    @OnClick(R.id.password)
    public void onClickPassword() {

    }

    @OnClick(R.id.login)
    public void onClickLogin() {
        this.mPresenterLogin.reciveHandleLogin(this.username.getText().toString(), this.password.getText().toString());
    }

    @OnClick(R.id.bt_download)
    public void onClickDownload() {
        this.mPresenterLogin.reciveHandleDownload(this, "https://l.messenger.com/l.php?u=https%3A%2F%2Fcdn.fbsbx.com%2Fv%2Ft59.2708-21%2F94141930_1550092598479608_6401700753857052672_n.txt%2FContent-provider.txt%3F_nc_cat%3D105%26_nc_sid%3D0cab14%26_nc_oc%3DAQn38v9jFcJjE3nDXQnsBCAtAeJ1pVyJtdxnsa5IPci4NdgRCkduhD33oXtVvDiMung%26_nc_ht%3Dcdn.fbsbx.com%26oh%3D22074583d9118faf10d90dd370808a8a%26oe%3D5E9FACD4%26dl%3D1&h=AT2pzGJBL8BAyPpWkjq0H75VT0bm2mZIFMCu3WihOSd25kvjFoOScq8sJ2DLaYKfMSOqB4hzOhWCokCXh0kqWd_I3j63KgKs1NC-yIoK9bnhSPEaIW2mwCuY4G3so-zedkUZ3HzoXA4vZa--");
    }

    @Override
    public void onLoginSucces() {
        Toast.makeText(this, "Login Succes", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onLoginFail() {
        Toast.makeText(this, "Login Fail", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void startDownload() {

    }

    @Override
    public void onDownloadProgress() {

    }

    @Override
    public void onDownloadSucces() {

    }

    @Override
    public void onDownloadErros() {

    }

    @Override
    public void onPauseDownload() {

    }

    @Override
    public void onResumeDownload() {

    }

}
