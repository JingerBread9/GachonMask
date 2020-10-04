package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.wonderkiln.camerakit.CameraKitError;
import com.wonderkiln.camerakit.CameraKitEvent;
import com.wonderkiln.camerakit.CameraKitEventListener;
import com.wonderkiln.camerakit.CameraKitImage;
import com.wonderkiln.camerakit.CameraKitVideo;
import com.wonderkiln.camerakit.CameraView;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

// 수정내용 여기 추가

/*
 * 0925 장호
 * 카메라 전면만 나오도록 수정
 * imageview 안쓰는것 같길래 레이아웃도 삭제함(주석처리)
 * 카메라 토글버튼 삭제
 *
 * 0928 장호
 * 수정하여 주석처리 된것들 그냥 깔끔하게 지움
 * 주석 번역, 코드 정렬
 *
 * 1004 장호
 * 마스크로 인식했을 시 토스트 메시지 출력하게 함
 * 코드 이해 어느정도 되서 필요한거 있으면 커스터마이징 가능할 듯
 * 일단 토스트 메시지 출력 조건을
 * 결과 상위 3개를 자동으로 출력하는데 거기에 "mask나 diaper 포함되면" 으로 해놓음
 */
public class SubActivity extends AppCompatActivity {

    private static final String MODEL_PATH = "mobilenet_quant_v1_224.tflite";
    private static final boolean QUANT = true;
    private static final String LABEL_PATH = "labels.txt";
    private static final int INPUT_SIZE = 224;

    private Classifier classifier;

    private Executor executor = Executors.newSingleThreadExecutor();
    private TextView textViewResult;
    private Button btnDetectObject;
    private CameraView cameraView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sub);
        cameraView = findViewById(R.id.cameraView);
        textViewResult = findViewById(R.id.textViewResult);
        btnDetectObject = findViewById(R.id.btnDetectObject);

        cameraView.addCameraKitListener(new CameraKitEventListener() {
            @Override
            public void onEvent(CameraKitEvent cameraKitEvent) {

            }

            @Override
            public void onError(CameraKitError cameraKitError) {

            }

            @Override
            public void onImage(CameraKitImage cameraKitImage) {

                Bitmap bitmap = cameraKitImage.getBitmap();

                bitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, false);

                final List<Classifier.Recognition> results = classifier.recognizeImage(bitmap);

                textViewResult.setText(results.toString());

                /**
                 * results는
                 * 추론 결과가 10퍼센트를 넘는 것 중
                 * 최대 3개의 결과를 가져오는데
                 *
                 * mask(mask, oxygen mask, ski mask, gasmask)
                 * diaper(기저귀라는 뜻: 난 흰색 덴탈 마스크 끼면 diaper로 인식하더라)
                 * 라는 단어를 포함하면
                 *
                 * 토스트 메시지 출력
                 * */
                for(int i=0;i<results.size();i++){
                    String title = results.get(i).getTitle();

                    if(title.contains("mask")||title.contains("diaper")) {
                        Toast.makeText(getApplicationContext(), "마스크 인식 완료~!", Toast.LENGTH_LONG).show();
                        finish();
                    }
                }
            }

            @Override
            public void onVideo(CameraKitVideo cameraKitVideo) {

            }
        });

        cameraView.toggleFacing();
        btnDetectObject.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cameraView.captureImage();
            }
        });

        initTensorFlowAndLoadModel();
    }

    @Override
    protected void onResume() {
        super.onResume();
        cameraView.start();
    }

    @Override
    protected void onPause() {
        cameraView.stop();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                classifier.close();
            }
        });
    }

    private void initTensorFlowAndLoadModel() {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    classifier = TensorFlowImageClassifier.create(getAssets(), MODEL_PATH, LABEL_PATH, INPUT_SIZE, QUANT);
                    makeButtonVisible();
                } catch (final Exception e) {
                    throw new RuntimeException("Error initializing TensorFlow!", e);
                }
            }
        });
    }

    private void makeButtonVisible() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                btnDetectObject.setVisibility(View.VISIBLE);
            }
        });
    }
}
