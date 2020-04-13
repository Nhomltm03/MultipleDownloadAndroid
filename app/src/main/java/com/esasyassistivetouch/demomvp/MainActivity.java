package com.esasyassistivetouch.demomvp;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.esasyassistivetouch.demomvp.ui.inter.IViewListener;
import com.esasyassistivetouch.demomvp.ui.presenter.PresenterLogin;

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
        this.mPresenterLogin.onDownloadErros();
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
