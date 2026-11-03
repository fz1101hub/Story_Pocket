import com.storypocket.mobile.TargetParser;
import com.storypocket.mobile.VideoUrlPolicy;
import com.storypocket.mobile.MediaTypePolicy;
import java.util.Arrays;

public final class OriginalVideoCheck {
    static int checked;
    static void yes(boolean b, String label) { if(!b) throw new AssertionError(label); checked++; }
    public static void main(String[] args) {
        String cdn = "https://scontent-mxp1-1.cdninstagram.com/o1/v/t16/f2/m86/a.mp4?stp=dst-mp4&token=123";
        yes(VideoUrlPolicy.score(cdn)>100,"CDN mp4 accettato");
        yes(VideoUrlPolicy.score("https://video.xx.fbcdn.net/v/t16/item?mime_type=video_mp4")>=70,"video MIME");
        yes(VideoUrlPolicy.score("https://scontent.cdninstagram.com/a.jpg")==0,"rifiuta jpeg");
        yes(VideoUrlPolicy.score("blob:https://www.instagram.com/a")==0,"rifiuta blob");
        yes(VideoUrlPolicy.score("http://scontent.cdninstagram.com/video.mp4")==0,"rifiuta HTTP");
        yes(VideoUrlPolicy.score("https://evil.example.com/video.mp4")==0,"rifiuta host esterno");
        yes(VideoUrlPolicy.score("https://scontent.cdninstagram.com/v/x.mpd")==0,"rifiuta MPD");
        yes(VideoUrlPolicy.score("https://scontent.cdninstagram.com/v/x.m4s")==0,"rifiuta M4S");
        yes(VideoUrlPolicy.score("https://scontent.cdninstagram.com/v/x.mp4?bytestart=0&byteend=123")==0,"rifiuta frammento range");
        yes(VideoUrlPolicy.score("https://scontent.cdninstagram.com/v/x.mp4?stp=dst-jpg")==0,"rifiuta copertina mascherata");
      ¶»§q«^