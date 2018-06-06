package com.example.jiwon.commonchat;
//[참조]https://www.androidtutorialpoint.com/firebase/real-time-android-chat-application-using-firebase-tutorial/

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

//채팅화면
public class ChatActivity extends AppCompatActivity implements View.OnClickListener {

    //계산기 버튼
    private Button button1,button2,button3,button4,button5,button6,button7,button8,button9,button10,button11,button12,button13,button14,button15,button16;
    private int result;      //계산기 결과값
    private int where=0;        //계산기 연산자 구분

    private static final String TAG = "ChatActivity";
    private Uri filePath;
    String imgmessageText;
    ImgMessageDTO imgmessage;

    SingleTouchView stv;
    private LinearLayout linear;
    private Button sendMemo;

//    private LinearLayout calculatorXML;

    private ScrollView scrollView;
    private LinearLayout layout;
    private ImageView sendButton;   // messagearea.xml에 존재하는 뷰
    private EditText messageArea;   // messagearea.xml에 존재하는 텍스트

    private DatabaseReference ref1, ref2;    // 데이터베이스를 참조하기 위한 선언
    private DatabaseReference ref;
    private FirebaseAuth mAuth;

    // db 방 이름
    static String roomname = "";
    static String roomother = "";

    String myname;
    String username;
    //유저이름과 메세지를 넣어줄 Map 정의
    MessageDTO message;
    Map<String, String> map = new HashMap<String, String>();
    String messageText;


    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(false);            //액션바 아이콘을 업 네비게이션 형태로 표시합니다.
        actionBar.setDisplayShowTitleEnabled(false);        //액션바에 표시되는 제목의 표시유무를 설정합니다.
        actionBar.setDisplayShowHomeEnabled(false);
        LayoutInflater inflater = (LayoutInflater)getSystemService(LAYOUT_INFLATER_SERVICE);
        View actionbar = inflater.inflate(R.layout.action_title, null);

        actionBar.setCustomView(actionbar);

        // 친구목록 액티비티에서의 other, myName 정보를 가져옴.
        Intent intent = getIntent();
        roomother = intent.getExtras().getString("other");
        myname = intent.getExtras().getString("myName");
        roomname = myname+"_"+roomother;
        Log.d("getNickname", roomname);

        // 데이터베이스에서 messages에 본인과 다른 유저의 데이터가 각각 남아 있을 수 있도록 각각 저장해주기.
        ref = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();
        ref1 = ref.child("messages").child(myname+"_"+roomother);
        ref2 = ref.child("messages").child(roomother+"_"+myname);


        button1 = (Button)findViewById(R.id.button1);
        button2 = (Button)findViewById(R.id.button2);
        button3 = (Button)findViewById(R.id.button3);
        button4 = (Button)findViewById(R.id.button4);
        button5 = (Button)findViewById(R.id.button5);
        button6 = (Button)findViewById(R.id.button6);
        button7 = (Button)findViewById(R.id.button7);
        button8 = (Button)findViewById(R.id.button8);
        button9 = (Button)findViewById(R.id.button9);
        button10 = (Button)findViewById(R.id.button10);
        button11 = (Button)findViewById(R.id.button11);
        button12 = (Button)findViewById(R.id.button12);
        button13 = (Button)findViewById(R.id.button13);
        button14 = (Button)findViewById(R.id.button14);
        button15 = (Button)findViewById(R.id.button15);
        button16 = (Button)findViewById(R.id.button16);


        layout = (LinearLayout) findViewById(R.id.layout);     // activity_chat의 LinearLayout
        sendButton = (ImageView) findViewById(R.id.sendButton);  // activity_chat의 LinearLayout
        messageArea = (EditText) findViewById(R.id.messageArea); // message_area의 EditText
        scrollView = (ScrollView) findViewById(R.id.scrollView); // activity_chat의 scrollView
        linear = (LinearLayout) findViewById(R.id.linear);

        // 그림메모 뷰를 현재 채팅뷰에 추가하는 부분
        stv = new SingleTouchView(this);
        stv.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,500));
        linear.addView(stv);

        if (mAuth != null) {

            ref.child("messages").child(roomname).addChildEventListener(new com.google.firebase.database.ChildEventListener() {
                @Override
                public void onChildAdded(com.google.firebase.database.DataSnapshot dataSnapshot, String s) {
                    MessageDTO messageDTO = dataSnapshot.getValue(MessageDTO.class);
                    // 데이터베이스에 저장 되어있는 유저이름과 전달받은 유저이름이 일치 할경우, 오른쪽 정렬
                    // 채팅을 하고 있는 자기 자신
                    if (messageDTO.getUser().equals(myname))
                        addMessageBox(messageDTO.getMessage(),1);
                        // 데이터베이스에 저장 되어있는 유저이름과 전달받은 유저이름이 불일치 할경우, 왼쪽 정렬
                        // 채팅을 하고 있는 상대방
                    else
                        addMessageBox(messageDTO.getMessage(),2);
                }

                @Override
                public void onChildChanged(com.google.firebase.database.DataSnapshot dataSnapshot, String s) {    }

                @Override
                public void onChildRemoved(com.google.firebase.database.DataSnapshot dataSnapshot) {   }

                @Override
                public void onChildMoved(com.google.firebase.database.DataSnapshot dataSnapshot, String s) {   }

                @Override
                public void onCancelled(DatabaseError databaseError) {  }
            });
        }

        // sendButton을 눌렀을 때에 입력된 문자를 messageArea에 setText해줌
        sendButton.setOnClickListener(this);
        button1.setOnClickListener(this);
        button2.setOnClickListener(this);
        button3.setOnClickListener(this);
        button4.setOnClickListener(this);
        button5.setOnClickListener(this);
        button6.setOnClickListener(this);
        button7.setOnClickListener(this);
        button8.setOnClickListener(this);
        button9.setOnClickListener(this);
        button10.setOnClickListener(this);
        button11.setOnClickListener(this);
        button12.setOnClickListener(this);
        button13.setOnClickListener(this);
        button14.setOnClickListener(this);
        button15.setOnClickListener(this);
        button16.setOnClickListener(this);

    }

    // 메세지 전송 버튼을 눌렀을때 데이터베이스가 저장됨.
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.sendButton:
                // 입력된 메세지내용 가져오기
                messageText = messageArea.getText().toString();
                // 메세지 내용이 존재할 경우
                if (!messageText.equals("")) {
                    // 데이터베이스에 유저이름과 메시지를 데이터베이스에 넣어준다.
                    ref.child("users").orderByChild("email").equalTo(mAuth.getCurrentUser().getEmail()).addChildEventListener(new com.google.firebase.database.ChildEventListener() {
                        @Override
                        public void onChildAdded(com.google.firebase.database.DataSnapshot dataSnapshot, String s) {
                            UserDTO userDTO = dataSnapshot.getValue(UserDTO.class);
                            username = userDTO.getName();
                            message = new MessageDTO(messageText, username);
                            ref1.push().setValue(message);   //데이터베이스에 값넣어주기
                            ref2.push().setValue(message);
                            messageArea.setText("");
                        }

                        @Override
                        public void onChildChanged(com.google.firebase.database.DataSnapshot dataSnapshot, String s) { }

                        @Override
                        public void onChildRemoved(com.google.firebase.database.DataSnapshot dataSnapshot) { }

                        @Override
                        public void onChildMoved(com.google.firebase.database.DataSnapshot dataSnapshot, String s) { }

                        @Override
                        public void onCancelled(DatabaseError databaseError) { }
                    });

                }
                break;

            // 계산기 숫자 입력
            case R.id.button1:
                messageArea.setText(messageArea.getText().toString()+1);
                break;
            case R.id.button2:
                messageArea.setText(messageArea.getText().toString()+2);
                break;
            case R.id.button3:
                messageArea.setText(messageArea.getText().toString()+3);
                break;
            case R.id.button4:      // 더하기
                result = Integer.valueOf(messageArea.getText().toString().trim());
                messageArea.setText("");
                where =1;
                break;

            // 계산기 숫자 입력
            case R.id.button5:
                messageArea.setText(messageArea.getText().toString()+4);
                break;
            case R.id.button6:
                messageArea.setText(messageArea.getText().toString()+5);
                break;
            case R.id.button7:
                messageArea.setText(messageArea.getText().toString()+6);
                break;
            case R.id.button8:      // 빼기
                result = Integer.valueOf(messageArea.getText().toString().trim());
                messageArea.setText("");
                where =2;
                break;
            // 계산기 숫자 입력
            case R.id.button9:
                messageArea.setText(messageArea.getText().toString()+7);
                break;
            case R.id.button10:
                messageArea.setText(messageArea.getText().toString()+8);
                break;
            case R.id.button11:
                messageArea.setText(messageArea.getText().toString()+9);
                break;
            case R.id.button12:      // 곱하기
                result = Integer.valueOf(messageArea.getText().toString().trim());
                messageArea.setText("");
                where =3;
                break;

            case R.id.button13:
                messageArea.setText(messageArea.getText().toString()+0);
                break;
            case R.id.button14:     //계산 결과 결과
                if(where==1){   //더한 결과
                    result = result + Integer.valueOf(messageArea.getText().toString().trim());
                    messageArea.setText(Integer.toString(result));
                }
                else if(where==2){
                    result = result - Integer.valueOf(messageArea.getText().toString().trim());
                    messageArea.setText(Integer.toString(result));
                }
                else if(where==3){
                    result = result * Integer.valueOf(messageArea.getText().toString().trim());
                    messageArea.setText(Integer.toString(result));
                }
                else if(where==4){
                    result = result / Integer.valueOf(messageArea.getText().toString().trim());
                    messageArea.setText(Integer.toString(result));
                }
                findViewById(R.id.calculatorXML).setVisibility(v.GONE);
                break;
            case R.id.button15:     //전체 지우기
                messageArea.setText("");
                break;
            case R.id.button16:     //나누기
                Integer.valueOf(messageArea.getText().toString().trim());
                messageArea.setText("");
                where =4;
                break;

        }
    }

    //storage
    FirebaseStorage storage = FirebaseStorage.getInstance();
    // 그림메모 저장하는 파일 이름명 정해주기
    SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMHH_mmss");
    Date now = new Date();
    String filename = formatter.format(now) + ".png";
    // 그림메모 이미지 파일 저장해주는 url 경로
    StorageReference storageRef = storage.getReferenceFromUrl("gs://commonchat-58d3d.appspot.com").child("images/" + filename); //ok


    // 그림메모 전송 버튼 누를시, 채팅방에 그림메모 출력
    public void onSendMemo(final View v) {

        // ChatActivity클래스에 텍스트뷰 생성(보내지는 대화메세지들을 화면에 띄우기 위해)
        final ImageView imgView = new ImageView(ChatActivity.this);

        File screenShot = ScreenShot(stv);          ////
        filePath = Uri.fromFile(screenShot);

        if (screenShot != null) {
            // storage에 업로드 진행 Dialog 보이기
            final ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.setTitle("업로드중...");
            progressDialog.show();

            //Storage에 파일 올리기
            storageRef.putFile(filePath)
                    //성공시
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            progressDialog.dismiss(); //업로드 진행 Dialog 상자 닫기

                            try {  // storage파일 내려받기
                                final File localFile = File.createTempFile("images", "jpg");
                                storageRef.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                                    //성공시 이미지 출력
                                    public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                                        Bitmap bitmap = BitmapFactory.decodeFile(localFile.getAbsolutePath());
                                        imgView.setImageBitmap(bitmap);


                                        // LayoutParams : 여백의 값(너비, 높이) 설정할 수 있게 해줌     // 현재 설정되어 있는 레이아웃 파라미터를 조사
                                        LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                                        lp2.weight = 1.0f;  // 가중치 주기

                                        imgView.setLayoutParams(lp2);      // textView(메세지)에 새로운 파라미터 적용
                                        layout.addView(imgView);           // LinearLayout에 textView 추가 시키기(주고받는 메시지 영역에 추가)
                                        scrollView.fullScroll(ScrollView.FOCUS_DOWN); // 스크롤 가장 아래로 보내기(채팅시, 가장 최근 대화내용이 보일 수 있도록)

                                        linear.setVisibility(v.GONE);
                                        sendMemo.setVisibility(v.GONE);

                                        final String imgmessageText = storageRef.toString();
                                        // 데이터베이스에 유저이름과 메시지를 데이터베이스에 넣어준다.

                                        if (!imgmessageText.equals("")) {

                                            ref.child("users").orderByChild("email").equalTo(mAuth.getCurrentUser().getEmail()).addChildEventListener(new com.google.firebase.database.ChildEventListener() {
                                                @Override
                                                public void onChildAdded(com.google.firebase.database.DataSnapshot dataSnapshot, String s) {
                                                    UserDTO userDTO = dataSnapshot.getValue(UserDTO.class);
                                                    username = userDTO.getName();
                                                    imgmessage = new ImgMessageDTO(imgmessageText, username);
                                                    ref1.push().setValue(message);   //데이터베이스에 값넣어주기
                                                    ref2.push().setValue(message);
                                                    messageArea.setText("");
                                                }

                                                @Override
                                                public void onChildChanged(com.google.firebase.database.DataSnapshot dataSnapshot, String s) {
                                                }

                                                @Override
                                                public void onChildRemoved(com.google.firebase.database.DataSnapshot dataSnapshot) {
                                                }

                                                @Override
                                                public void onChildMoved(com.google.firebase.database.DataSnapshot dataSnapshot, String s) {
                                                }

                                                @Override
                                                public void onCancelled(DatabaseError databaseError) {
                                                }
                                            });

                                        }




                                    }

                                    // 실패시 메세지 출력
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception exception) {

                                    }// progress 진행 상황
                                }).addOnProgressListener(new OnProgressListener<FileDownloadTask.TaskSnapshot>() {
                                    @Override
                                    public void onProgress(FileDownloadTask.TaskSnapshot taskSnapshot) {
                                        @SuppressWarnings("VisibleForTests")
                                        double progress = (100 * taskSnapshot.getBytesTransferred()) /  taskSnapshot.getTotalByteCount();
                                    }
                                });

                            } catch (IOException e ) {
                                e.printStackTrace();
                            }

                        }

                    })
                    //실패시
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            progressDialog.dismiss();
                            Toast.makeText(getApplicationContext(), "업로드 실패!", Toast.LENGTH_SHORT).show();
                        }
                    })
                    //진행중
                    .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                            @SuppressWarnings("VisibleForTests")
                            double progress = (100 * taskSnapshot.getBytesTransferred()) /  taskSnapshot.getTotalByteCount();
                            //dialog에 진행률을 퍼센트로 출력해 준다
                            progressDialog.setMessage("Uploaded " + ((int) progress) + "% ...");
                        }
                    });

        } else {
            Toast.makeText(getApplicationContext(), "그림을 먼저 그려주세요.", Toast.LENGTH_SHORT).show();
        }




    }

    // 채팅내용을 TextView에 담아 채팅방에 출력
    public void addMessageBox(String message, int type) {
        // ChatActivity클래스에 텍스트뷰 생성(보내지는 대화메세지들을 화면에 띄우기 위해)
        TextView textView = new TextView(ChatActivity.this);
        textView.setText(message);

        Typeface typeface = getResources().getFont(R.font.font_bmjua);      // API레벨과 폰트가 맞지않아 오류가 뜨는 것이므로 무시해도 무방함.
        textView.setTypeface(typeface);
        textView.setTextSize(24);
        textView.setPadding(10,10,10,10);

        // LayoutParams : 여백의 값(너비, 높이) 설정할 수 있게 해줌     // 현재 설정되어 있는 레이아웃 파라미터를 조사
        LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp2.weight = 1.0f;  // 가중치 주기

        if (type == 1) {   //LinearLayout에 해당하는 속성은 오른쪽정렬
            lp2.gravity = Gravity.RIGHT;
            textView.setBackgroundResource(R.drawable.rounded_corner1);

        } else {   //LinearLayout에 해당하는 속성은 왼쪽정렬
            lp2.gravity = Gravity.LEFT;
            textView.setBackgroundResource(R.drawable.rounded_corner2);
        }

        textView.setLayoutParams(lp2);      // textView(메세지)에 새로운 파라미터 적용
        layout.addView(textView);           // LinearLayout에 textView 추가 시키기(주고받는 메시지 영역에 추가)
        scrollView.fullScroll(ScrollView.FOCUS_DOWN); // 스크롤 가장 아래로 보내기(채팅시, 가장 최근 대화내용이 보일 수 있도록)
    }


    // 그림 메모를 ImageView에 담아 채팅방에 출력
    public void addImgMessageBox(Uri uri, int type) {


    }


    // 계산기, 그림메모 메뉴 보여주기
    public void moreMenu(View v) {
        LinearLayout selectMenu = findViewById(R.id.selectMenu);
        selectMenu.setVisibility(v.VISIBLE);
    }

    // 그림메모 화면 보여주기
    public void onMemo(View v) {
        LinearLayout selectMenu = (LinearLayout) findViewById(R.id.selectMenu);
        selectMenu.setVisibility(v.GONE);

        linear = (LinearLayout) findViewById(R.id.linear);
        linear.setVisibility(v.VISIBLE);

        sendMemo = (Button) findViewById(R.id.sendMemo);
        sendMemo.setVisibility(v.VISIBLE);

    }

    // 계산기 화면 보여주기
    public void onCal(View v) {
        LinearLayout selectMenu = (LinearLayout) findViewById(R.id.selectMenu);
        selectMenu.setVisibility(v.GONE);

        LinearLayout calculatorXML = (LinearLayout) findViewById(R.id.calculatorXML);
        calculatorXML.setVisibility(v.VISIBLE);



    }


    // 그림메모 부분만 캡쳐하기
    private File ScreenShot(View view) {
        stv.setDrawingCacheEnabled(true);    // 캐쉬허용

        // 캐쉬에서 가져온 비트맵을 복사해서 새로운 비트맵(스크린샷) 생성
        Bitmap screenshot = Bitmap.createBitmap(stv.getDrawingCache());             /////

        // SDCard(ExternalStorage) : 외부저장공간
        File file = new File(Environment.getExternalStorageDirectory()+"/Pictures", filename);

        FileOutputStream fos;
        try {
            fos = new FileOutputStream(file);
            screenshot.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();
            Toast.makeText(this, "저장 성공", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "저장 실패", Toast.LENGTH_SHORT).show();
        }

        stv.setDrawingCacheEnabled(false);   // 캐쉬닫기
        return file;
    }



    // 그림메모가 가능한 뷰
    class SingleTouchView extends View {
        Paint paint = new Paint();
        Path path = new Path();
        public SingleTouchView(Context context) {
            super(context);

            paint.setAntiAlias(true);
            paint.setStrokeWidth(10f);
            paint.setColor(Color.BLACK);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeJoin(Paint.Join.ROUND);
        }

        //  선그리기
        protected void onDraw(Canvas canvas) {
            canvas.drawPath(path, paint);
        }

        public boolean onTouchEvent(MotionEvent event) {
            float eventX = event.getX();
            float eventY = event.getY();

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN :
                    path.moveTo(eventX, eventY);
                    return true;
                case MotionEvent.ACTION_MOVE :
                    path.lineTo(eventX, eventY);
                    break;
                case MotionEvent.ACTION_UP :
                    break;
                default:
                    return false;
            }
            invalidate();       // 새로운을 선을 그을 수 있도록 화면 갱신
            return true;
        }
    }


}
