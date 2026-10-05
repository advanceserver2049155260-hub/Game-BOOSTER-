package com.gamebooster.pro;

import android.app.*;
import android.app.usage.*;
import android.content.*;
import android.content.pm.*;
import android.graphics.*;
import android.net.*;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.Toast;
import java.util.*;
import java.util.concurrent.*;

public class MainActivity extends Activity {
    BoosterView view;
    ActivityManager am;
    BatteryReceiver batteryReceiver;
    long lastFreeBytes = -1;
    int tempC = 0;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.rgb(5,9,18));
        getWindow().setNavigationBarColor(Color.rgb(5,9,18));
        am = (ActivityManager)getSystemService(ACTIVITY_SERVICE);
        view = new BoosterView(this);
        setContentView(view);
        registerBattery();
    }

    void registerBattery() {
        batteryReceiver = new BatteryReceiver();
        registerReceiver(batteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }

    @Override protected void onDestroy() {
        if (batteryReceiver != null) unregisterReceiver(batteryReceiver);
        super.onDestroy();
    }

    class BatteryReceiver extends BroadcastReceiver {
        public void onReceive(Context c, Intent i) {
            int t = i.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0);
            tempC = Math.round(t / 10f);
            if (view != null) view.invalidate();
        }
    }

    long availableRam() {
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(mi);
        return mi.availMem;
    }

    long totalRam() {
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(mi);
        return mi.totalMem;
    }

    void optimizeRam() {
        lastFreeBytes = availableRam();
        // Android intentionally limits what third-party apps can terminate.
        // This is best-effort and does not touch game memory/files.
        List<ActivityManager.RunningAppProcessInfo> processes = am.getRunningAppProcesses();
        int attempted = 0;
        if (processes != null) {
            for (ActivityManager.RunningAppProcessInfo p : processes) {
                if (p.importance >= ActivityManager.RunningAppProcessInfo.IMPORTANCE_BACKGROUND
                        && p.pkgList != null) {
                    for (String pkg : p.pkgList) {
                        if (!pkg.equals(getPackageName())) {
                            try { am.killBackgroundProcesses(pkg); attempted++; } catch (Exception ignored) {}
                        }
                    }
                }
            }
        }
        Toast.makeText(this, "RAM optimization requested for " + attempted + " background packages", Toast.LENGTH_SHORT).show();
        view.invalidate();
    }

    void openGameList() {
        Intent i = new Intent(Intent.ACTION_MAIN);
        i.addCategory(Intent.CATEGORY_LAUNCHER);
        startActivity(i);
    }

    void openSystemBattery() {
        try { startActivity(new Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS)); }
        catch (Exception e) { startActivity(new Intent(Settings.ACTION_SETTINGS)); }
    }

    class BoosterView extends View {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
        float d;
        int page = 0;
        String[] nav = {"HOME","GAMES","TOOLS","SETTINGS"};

        BoosterView(Context c) {
            super(c);
            d = getResources().getDisplayMetrics().density;
            p.setTypeface(Typeface.create("sans", Typeface.NORMAL));
            stroke.setStyle(Paint.Style.STROKE);
            setBackgroundColor(Color.rgb(5,9,18));
        }

        float dp(float x){ return x*d; }
        void txt(Canvas c,String s,float x,float y,float size,int color,Paint.Align align){
            p.setStyle(Paint.Style.FILL); p.setTextSize(dp(size)); p.setColor(color);
            p.setTextAlign(align); p.setTypeface(Typeface.create("sans", Typeface.NORMAL));
            c.drawText(s,dp(x),dp(y),p);
        }
        void bold(Canvas c,String s,float x,float y,float size,int color,Paint.Align align){
            p.setStyle(Paint.Style.FILL); p.setTextSize(dp(size)); p.setColor(color);
            p.setTextAlign(align); p.setTypeface(Typeface.create("sans", Typeface.BOLD));
            c.drawText(s,dp(x),dp(y),p);
        }
        void round(Canvas c,float l,float t,float r,float b,float rad,int color){
            p.setStyle(Paint.Style.FILL); p.setColor(color);
            c.drawRoundRect(dp(l),dp(t),dp(r),dp(b),dp(rad),dp(rad),p);
        }
        void ring(Canvas c,float cx,float cy,float radius,float progress,int color){
            stroke.setStrokeWidth(dp(7)); stroke.setStrokeCap(Paint.Cap.ROUND);
            stroke.setColor(Color.rgb(25,45,60)); stroke.setStyle(Paint.Style.STROKE);
            c.drawCircle(dp(cx),dp(cy),dp(radius),stroke);
            stroke.setColor(color);
            RectF r = new RectF(dp(cx-radius),dp(cy-radius),dp(cx+radius),dp(cy+radius));
            c.drawArc(r,-90,progress*360,false,stroke);
        }

        @Override protected void onDraw(Canvas c){
            super.onDraw(c);
            float w=getWidth()/d, h=getHeight()/d;
            p.setShader(new LinearGradient(0,0,getWidth(),getHeight(),
                    Color.rgb(4,15,28),Color.rgb(5,8,18),Shader.TileMode.CLAMP));
            c.drawRect(0,0,getWidth(),getHeight(),p); p.setShader(null);

            if(page==0) drawHome(c,w,h);
            else if(page==1) drawGames(c,w,h);
            else if(page==2) drawTools(c,w,h);
            else drawSettings(c,w,h);
            drawNav(c,w,h);
        }

        void header(Canvas c,String title){
            bold(c,title,22,38,22,Color.WHITE,Paint.Align.LEFT);
            txt(c,"GAME BOOSTER PRO",22,58,10,Color.rgb(0,245,160),Paint.Align.LEFT);
        }

        void drawHome(Canvas c,float w,float h){
            header(c,"Gaming Dashboard");
            ring(c,w/2,170,92,.82f,Color.rgb(0,245,160));
            bold(c,"BOOST",w/2,164,23,Color.rgb(0,245,160),Paint.Align.CENTER);
            txt(c,"ONE-TAP OPTIMIZATION",w/2,184,10,Color.LTGRAY,Paint.Align.CENTER);

            round(c,38,225,w-38,278,28,Color.rgb(0,220,145));
            bold(c,"BOOST NOW",w/2,258,16,Color.rgb(0,20,16),Paint.Align.CENTER);

            long avail=availableRam(), total=totalRam();
            float used=Math.max(0,total-avail);
            card(c,18,300,w/2-8,385,"RAM",formatGb(used)+" / "+formatGb(total),
                    "Available "+formatGb(avail),Color.rgb(0,245,160));
            card(c,w/2+8,300,w-18,385,"TEMP",tempC+"°C","Battery/CPU reading",Color.rgb(70,170,255));

            smallCard(c,18,405,w/2-8,475,"🧹","FREE RAM","Optimize");
            smallCard(c,w/2+8,405,w-18,475,"⚡","PERFORMANCE","Mode");
            smallCard(c,18,490,w/2-8,560,"🎮","GAMES","Launcher");
            smallCard(c,w/2+8,490,w-18,560,"📶","NETWORK","Ping");

            bold(c,"Safe mode",18,600,13,Color.WHITE,Paint.Align.LEFT);
            txt(c,"No game files, memory injection or anti-cheat bypass.",18,621,11,Color.LTGRAY,Paint.Align.LEFT);
        }

        void card(Canvas c,float l,float t,float r,float b,String a,String b1,String b2,int accent){
            round(c,l,t,r,b,18,Color.rgb(11,22,34));
            bold(c,a,l+14,t+25,11,accent,Paint.Align.LEFT);
            bold(c,b1,l+14,t+53,18,Color.WHITE,Paint.Align.LEFT);
            txt(c,b2,l+14,t+73,10,Color.LTGRAY,Paint.Align.LEFT);
        }

        void smallCard(Canvas c,float l,float t,float r,float b,String icon,String title,String sub){
            round(c,l,t,r,b,16,Color.rgb(10,20,31));
            txt(c,icon,l+16,t+28,18,Color.WHITE,Paint.Align.LEFT);
            bold(c,title,l+16,t+50,10,Color.WHITE,Paint.Align.LEFT);
            txt(c,sub,l+16,t+64,9,Color.LTGRAY,Paint.Align.LEFT);
        }

        void drawGames(Canvas c,float w,float h){
            header(c,"Game Launcher");
            round(c,18,80,w-18,125,22,Color.rgb(10,23,36));
            txt(c,"Search installed games",35,108,12,Color.GRAY,Paint.Align.LEFT);
            bold(c,"LAUNCHER",18,160,12,Color.rgb(0,245,160),Paint.Align.LEFT);
            round(c,18,180,w-18,245,18,Color.rgb(12,25,38));
            bold(c,"Open Android Game Launcher",32,208,14,Color.WHITE,Paint.Align.LEFT);
            txt(c,"Choose a game from your installed apps",32,228,10,Color.LTGRAY,Paint.Align.LEFT);
            round(c,18,270,w-18,325,18,Color.rgb(0,220,145));
            bold(c,"OPEN GAMES",w/2,303,14,Color.rgb(0,20,16),Paint.Align.CENTER);
            txt(c,"Tip: add your favorite game to your launcher/home screen.",18,365,11,Color.LTGRAY,Paint.Align.LEFT);
        }

        void drawTools(Canvas c,float w,float h){
            header(c,"Gaming Tools");
            smallCard(c,18,85,w/2-8,160,"🧹","FREE RAM","Real system reading");
            smallCard(c,w/2+8,85,w-18,160,"🌡","THERMAL","Temperature");
            smallCard(c,18,175,w/2-8,250,"📊","FPS","Device/API support");
            smallCard(c,w/2+8,175,w-18,250,"📶","PING","Network monitor");
            smallCard(c,18,265,w/2-8,340,"🔋","BATTERY","Gaming mode");
            smallCard(c,w/2+8,265,w-18,340,"🔕","DND","Notifications");
            round(c,18,370,w-18,425,18,Color.rgb(12,25,38));
            bold(c,"RAM OPTIMIZATION",32,402,13,Color.WHITE,Paint.Align.LEFT);
            txt(c,"Best-effort Android background-process cleanup.",32,420,9,Color.LTGRAY,Paint.Align.LEFT);
            round(c,18,445,w-18,500,18,Color.rgb(0,220,145));
            bold(c,"FREE RAM NOW",w/2,479,14,Color.rgb(0,20,16),Paint.Align.CENTER);
        }

        void drawSettings(Canvas c,float w,float h){
            header(c,"Settings");
            setting(c,85,"Auto Boost","Off");
            setting(c,145,"Gaming Notifications","On");
            setting(c,205,"Keep Screen Awake","On");
            setting(c,265,"Battery Saver","System");
            setting(c,325,"Theme","Dark Neon");
            setting(c,385,"Safety","Non-invasive");
            round(c,18,455,w-18,510,18,Color.rgb(12,25,38));
            txt(c,"Game Booster Pro v1.0",32,488,11,Color.LTGRAY,Paint.Align.LEFT);
        }
        void setting(Canvas c,float y,String a,String b){
            round(c,18,y, getWidth()/d-18,y+46,14,Color.rgb(10,20,31));
            txt(c,a,32,y+29,12,Color.WHITE,Paint.Align.LEFT);
            txt(c,b,getWidth()/d-32,y+29,10,Color.rgb(0,245,160),Paint.Align.RIGHT);
        }

        void drawNav(Canvas c,float w,float h){
            float top=h-72;
            round(c,10,top,w-10,h-8,22,Color.rgb(8,17,27));
            for(int i=0;i<4;i++){
                float x=w*(i+.5f)/4f;
                int col=(page==i?Color.rgb(0,245,160):Color.GRAY);
                String icon=new String[]{"⌂","🎮","⚙","☰"}[i];
                txt(c,icon,x,top+28,18,col,Paint.Align.CENTER);
                txt(c,nav[i],x,top+50,8,col,Paint.Align.CENTER);
            }
        }

        String formatGb(long bytes){
            return String.format(Locale.US,"%.1f GB",bytes/1073741824.0);
        }

        @Override public boolean onTouchEvent(android.view.MotionEvent e){
            if(e.getAction()!=MotionEvent.ACTION_UP) return true;
            float x=e.getX()/d, y=e.getY()/d, w=getWidth()/d, h=getHeight()/d;

            if(y>h-90){
                page=Math.min(3,Math.max(0,(int)(x/(w/4))));
                invalidate(); return true;
            }
            if(page==0 && y>=220 && y<=290){
                optimizeRam(); return true;
            }
            if(page==0 && y>=400 && y<=480 && x<w/2){ optimizeRam(); return true; }
            if(page==0 && y>=400 && y<=480 && x>=w/2){ 
                Toast.makeText(MainActivity.this,"Performance mode uses safe Android settings only.",Toast.LENGTH_SHORT).show(); return true;
            }
            if(page==1 && y>=260 && y<=340){ openGameList(); return true; }
            if(page==2 && y>=360 && y<=520){ optimizeRam(); return true; }
            return true;
        }
    }
}
