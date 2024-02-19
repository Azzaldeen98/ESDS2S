package com.example.esds2s.ApiClient;
import java.io.IOException;
import kotlinx.coroutines.*;
import okhttp3.*;

public class TextToSpeechService {

    public static Response sendRequest(String input_text) throws IOException {

        OkHttpClient client =new OkHttpClient();
        MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
        RequestBody body = RequestBody.create(mediaType, "voice_code=ar-SA-3&text="+input_text+"&speed=1.00&pitch=1.00&output_type=audio_url");
        Request request = new Request.Builder()
                .url("https://cloudlabs-text-to-speech.p.rapidapi.com/synthesize")
                .post(body)
                .addHeader("content-type", "application/x-www-form-urlencoded")
                .addHeader("X-RapidAPI-Key", "e8af95d120msha76214f99ebe838p1ad208jsnda6e1e2cd7d9")
                .addHeader("X-RapidAPI-Host", "cloudlabs-text-to-speech.p.rapidapi.com")
                .build();

        Response response = client.newCall(request).execute();
        return response;
    }

}
