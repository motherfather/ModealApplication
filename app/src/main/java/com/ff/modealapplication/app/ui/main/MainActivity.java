package com.ff.modealapplication.app.ui.main;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.login.LoginManager;
import com.ff.modealapplication.R;
import com.ff.modealapplication.andorid.network.SafeAsyncTask;
import com.ff.modealapplication.app.core.service.MainService;
import com.ff.modealapplication.app.core.util.BackPressCloseHandler;
import com.ff.modealapplication.app.core.util.GPSPreference;
import com.ff.modealapplication.app.core.util.LoginPreference;
import com.ff.modealapplication.app.core.util.RecyclerItemClickListener;
import com.ff.modealapplication.app.ui.bookmark.BookmarkActivity;
import com.ff.modealapplication.app.ui.help.HelpActivity;
import com.ff.modealapplication.app.ui.item.ItemActivity;
import com.ff.modealapplication.app.ui.item.ItemDetailActivity;
import com.ff.modealapplication.app.ui.join.JoinActivity;
import com.ff.modealapplication.app.ui.join.JoinLeaveActivity;
import com.ff.modealapplication.app.ui.login.LoginActivity;
import com.ff.modealapplication.app.ui.map.SearchMapRangeActivity;
import com.ff.modealapplication.app.ui.message.AlarmActivity;
import com.ff.modealapplication.app.ui.mypage.OwnerMyPageActivity;
import com.ff.modealapplication.app.ui.mypage.UserMyPageActivity;
import com.ff.modealapplication.app.ui.notice.NoticeActivity;
import com.ff.modealapplication.app.ui.search.SearchActivity;

import java.util.List;
import java.util.Map;

import static android.location.LocationManager.GPS_PROVIDER;
import static android.location.LocationManager.NETWORK_PROVIDER;
import static com.ff.modealapplication.R.id.nav_manage;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, View.OnClickListener, AdapterView.OnItemClickListener {
    NavigationView navigationView;
    boolean flag_withdraw = false;
    SwipeRefreshLayout layout;      // refresh
    int i = 0;                      // refresh count

    //    ListView listView = null; // 리사이클러뷰 쓰면서 제거
    MainListArrayAdapter mainListArrayAdapter = null;

    RecyclerView recyclerView;
//    List<Map<String, Object>> mainList; // 갱신을 async안에 넣으면서 필요하지 않아짐

    private BackPressCloseHandler backPressCloseHandler;
    private DrawerLayout drawer = null;

    // 지도
    double longitude = 0; // 위도
    double latitude = 0; // 경도
    LocationManager lm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        startActivity(new Intent(this, SplashActivity.class)); // 스플래시 화면 (170131/상욱추가)

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        layout = (SwipeRefreshLayout) findViewById(R.id.layout);
        layout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                // 새로고침 작업 진행
                new MainListAsyncTask().execute();
                layout.setRefreshing(false);        // 새로고침 작업 끝난 후 새로고침 중단
            }
        });

        backPressCloseHandler = new BackPressCloseHandler(this);

        // 프래그먼트에서 떼오고 없애버림
//        mainListArrayAdapter = new MainListArrayAdapter(this);
//        listView = (ListView) findViewById(R.id.main_list);
//        listView.setAdapter(mainListArrayAdapter);
//        listView.setOnItemClickListener(this);

        // 리스트뷰 대신 리사이클러뷰 사용...
        recyclerView = (RecyclerView) findViewById(R.id.main_list);
        mainListArrayAdapter = new MainListArrayAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext(), LinearLayoutManager.VERTICAL, false));
        recyclerView.setAdapter(mainListArrayAdapter);
        recyclerView.addOnItemTouchListener(new RecyclerItemClickListener(this, recyclerView, new RecyclerItemClickListener.OnItemClickListener() { // by.Jacob Tabak 엄청나다...
            @Override
            public void onItemClick(View view, int position) {
                // do whatever
                Intent intent = new Intent(getApplicationContext(), ItemDetailActivity.class);
                intent.putExtra("no", (Double.valueOf(((TextView) view.findViewById(R.id.send_no)).getText().toString())).longValue());
                intent.putExtra("shopNo", (Double.valueOf(((TextView) view.findViewById(R.id.send_shopNo)).getText().toString())).longValue());
                startActivity(intent);
                onPause();
            }

            @Override
            public void onLongItemClick(View view, int position) {
                // do whatever
            }
        }));

        //listview footer // 리사이클러뷰 푸터를 달다가는 다른코드 다터질것 같아서... 보류...
//        View footer = getLayoutInflater().inflate(R.layout.list_footer, null, false);
//        listView.addFooterView(footer);
//        listView.setFooterDividersEnabled(false);

        new MainListAsyncTask().execute();
        // 로고 클릭시 새로고침
        findViewById(R.id.logo_id).setOnClickListener(this);

        // ---------------- GPS ----------------
        // LocationManager 객체를 얻어온다
        lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        gps();
        // ---------------- GPS ----------------

        final LayoutInflater inflater = getLayoutInflater();

        findViewById(R.id.fab).setOnClickListener(new View.OnClickListener() { // 플로팅 버튼 클릭시
            @Override
            public void onClick(View view) {
                findViewById(R.id.fab).setVisibility(View.INVISIBLE);
                final Snackbar snackbar = Snackbar.make(findViewById(R.id.coordinator), "", Snackbar.LENGTH_INDEFINITE); // 스낵바 띄움
                Snackbar.SnackbarLayout layout = (Snackbar.SnackbarLayout) snackbar.getView();
                snackbar.getView().getViewTreeObserver().addOnScrollChangedListener(new ViewTreeObserver.OnScrollChangedListener() { // 스낵바 켜졌을때 스크롤 돌릴때 스낵바 닫기
                    @Override
                    public void onScrollChanged() {
                        snackbar.dismiss();
                        findViewById(R.id.fab).setVisibility(View.VISIBLE);
                    }
                });
                snackbar.getView().getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() { // 스낵바 스와이프(밀어서 끄는 기능) 막음... (시크바를 위해서...)
                    @Override
                    public boolean onPreDraw() {
                        snackbar.getView().getViewTreeObserver().removeOnPreDrawListener(this);
                        ((CoordinatorLayout.LayoutParams) snackbar.getView().getLayoutParams()).setBehavior(null);
                        return true;
                    }
                });
                layout.setBackgroundColor(Color.parseColor("#ff8E99"));
                TextView textView = (TextView) layout.findViewById(android.support.design.R.id.snackbar_text);
                textView.setVisibility(View.INVISIBLE); // 스낵바 텍스트 안보이기
                View snackView = inflater.inflate(R.layout.my_snackbar, null); // 스낵바 커스텀 레이아웃
                SeekBar seekBar = (SeekBar) snackView.findViewById(R.id.snackbar_seekBar); // 시크바
                seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
                        ((TextView) findViewById(R.id.snackbar_distance)).setText("검색 거리 설정  " + progress * 30 + "m");
                        GPSPreference.put(getApplicationContext(), progress * 30);
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
//                        try {
//                            Thread.sleep(1000); // 딜레이 주기
//                        } catch (Exception e) {
//                            e.printStackTrace();
//                        }
//                        snackbar.dismiss();
                    }
                });
                Button button = (Button) snackView.findViewById(R.id.snackbar_button); // 자기 위치정보 찾기 버튼
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        gps();
                        snackbar.dismiss();
                        findViewById(R.id.fab).setVisibility(View.VISIBLE);
                    }
                });
                layout.addView(snackView, 0); // 뭔지 모르겠음
                snackbar.show(); // 스냅바
            }
        });

        // 툴바 추가 (170123/상욱추가)
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false); // 타이틀에 있는 앱이름 숨기기

        // 네비게이션 추가(170123/상욱추가)
        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle); // setDrawerListener가 addDrawerListener로 바뀜 (170131/준현수정)
        toggle.syncState();

        // 로그인 클릭시
        ((NavigationView) findViewById(R.id.nav_view)).getHeaderView(0).findViewById(R.id.login_button).setOnClickListener(this);

        // 회원가입 클릭시
        ((NavigationView) findViewById(R.id.nav_view)).getHeaderView(0).findViewById(R.id.register_button).setOnClickListener(this);

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Intent intent = new Intent(this, ItemDetailActivity.class);
        intent.putExtra("no", (Double.valueOf(((TextView) view.findViewById(R.id.send_no)).getText().toString())).longValue());
        intent.putExtra("shopNo", (Double.valueOf(((TextView) view.findViewById(R.id.send_shopNo)).getText().toString())).longValue());
        startActivity(intent);
        onPause();
    }

    // 리스트 띄우는 AsyncTask
    private class MainListAsyncTask extends SafeAsyncTask<List<Map<String, Object>>> {
        @Override
        public List<Map<String, Object>> call() throws Exception {
            MainService mainService = new MainService();
            List<Map<String, Object>> list = mainService.MainItemList((String) GPSPreference.getValue(getApplicationContext(), "latitude"), (String) GPSPreference.getValue(getApplicationContext(), "longitude"), (int) GPSPreference.getValue(getApplicationContext(), "range")); // 위도, 경도, 반경
            return list;
        }

        @Override
        protected void onSuccess(List<Map<String, Object>> list) throws Exception {
            mainListArrayAdapter.swap(list); // 갱신을 안에 넣으면서 문제가 해결된듯...
//            mainList = list;
            mainListArrayAdapter.add(list);
            super.onSuccess(list);
        }

        @Override
        protected void onException(Exception e) throws RuntimeException {
            Log.d("*Main Exception error :", "" + e);
            throw new RuntimeException(e);
        }
    }

    // GPS정보 여기부터 (170211/상욱추가)
    private LocationListener locationListener = new LocationListener() {

        @Override
        public void onLocationChanged(Location location) {
            // 여기서 위치값이 갱신되면 이벤트가 발생한다.
            // 값은 Location 형태로 리턴되며 좌표 출력 방법은 다음과 같다.
            // 위치 정보를 가져올 수 있는 메소드입니다.
            // 위치 이동이나 시간 경과 등으로 인해 호출됩니다.
            // 최신 위치는 location 파라메터가 가지고 있습니다.
            // 최신 위치를 가져오려면, location 파라메터를 이용하시면 됩니다.

            lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

            Log.d("test", "onLocationChanged, location:" + location);

            latitude = location.getLatitude();   //위도
            longitude = location.getLongitude(); //경도

            double altitude = location.getAltitude();   //고도
            float accuracy = location.getAccuracy();    //정확도
            String provider = location.getProvider();   //위치제공자
            /***********
             Gps 위치제공자에 의한 위치변화. 오차범위가 좁다.
             Network 위치제공자에 의한 위치변화
             Network 위치는 Gps에 비해 정확도가 많이 떨어진다.
             **********/

            // checkSelfPermission이 권한 확인
            if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // requestPermissions가 권한 요청(안드로이드 6.0이상부터 대화상자가 표시된다고 함...)
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{/*android.*/Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
            }

            // GPS 정보를 얻어서 쿠키로 저장
            GPSPreference.put(getApplicationContext(), String.valueOf(latitude), String.valueOf(longitude));
            Toast.makeText(getApplicationContext(), GPSPreference.getValue(getApplicationContext(), "latitude") + " : " + GPSPreference.getValue(getApplicationContext(), "longitude") + " : " + GPSPreference.getValue(getApplicationContext(), "range"), Toast.LENGTH_SHORT).show();
            lm.removeUpdates(this); // GPS 정지
            new MainListAsyncTask().execute();
//            mainListArrayAdapter.swap(mainList);
//            mainListArrayAdapter.notifyItemRangeRemoved(0, mainList.size()); // 리스트 갱신을 위한 다 지우기 (리사이클러뷰)
//            mainListArrayAdapter.clear(); // 리스트 갱신을 위한 다 지우기 (리스트뷰)
//            mainListArrayAdapter.notifyDataSetChanged(); // 리스트 갱신을 위한 값 변경시 알려주기?
        }

        @Override
        public void onProviderDisabled(String provider) {
            // 위치 공급자가 사용 불가능해질(disabled) 때 호출 됩니다.
            // 단순히 위치 정보를 구한다면, 코드를 작성하실 필요는 없습니다.
            Log.d("test", "onProviderDisabled, provider:" + provider);
        }

        @Override
        public void onProviderEnabled(String provider) {
            // 위치 공급자가 사용 가능해질(enabled) 때 호출 됩니다.
            // 단순히 위치 정보를 구한다면, 코드를 작성하실 필요는 없습니다.
            Log.d("test", "onProviderEnabled, provider:" + provider);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            // 위치 공급자의 상태가 바뀔 때 호출 됩니다.
            // 단순히 위치 정보를 구한다면, 코드를 작성하실 필요는 없습니다.
            Log.d("test", "onStatusChanged, provider:" + provider + ", status:" + status + " ,Bundle:" + extras);
        }
    };
// GPS정보 여기까지

    @Override
    protected void onStart() {
        super.onStart();

        // 로그인시 보임
        if (LoginPreference.getValue(getApplicationContext(), "id") != null) {
            ((Button) ((NavigationView) findViewById(R.id.nav_view)).getHeaderView(0).findViewById(R.id.login_button)).setText("로그아웃");
            ((NavigationView) findViewById(R.id.nav_view)).getHeaderView(0).findViewById(R.id.register_button).setVisibility(View.GONE);
            if ((Long) LoginPreference.getValue(getApplicationContext(), "shopNo") != -1) { // 사업자 로그인시 보임
                ((NavigationView) findViewById(R.id.nav_view)).getMenu().findItem(R.id.nav_manage).setVisible(true);
            } else { // 일반 사용자 로그인시
                ((NavigationView) findViewById(R.id.nav_view)).getMenu().findItem(R.id.nav_manage).setVisible(false);
            }
            flag_withdraw = true;
        } else if (LoginPreference.getValue(getApplicationContext(), "id") == null) { // 비로그인시
            ((Button) ((NavigationView) findViewById(R.id.nav_view)).getHeaderView(0).findViewById(R.id.login_button)).setText("로그인");
            ((NavigationView) findViewById(R.id.nav_view)).getHeaderView(0).findViewById(R.id.register_button).setVisibility(View.VISIBLE);
            ((NavigationView) findViewById(R.id.nav_view)).getMenu().findItem(R.id.nav_manage).setVisible(false);
            flag_withdraw = false;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (LoginPreference.getValue(getApplicationContext(), "id") != null) {
            flag_withdraw = true;
        } else if (LoginPreference.getValue(getApplicationContext(), "id") == null) {
            flag_withdraw = false;
        }

        // 회원 탈퇴 보이기/보이지않기
        navigationView = (NavigationView) findViewById(R.id.nav_view);
        Menu nav_menu = navigationView.getMenu();
        nav_menu.findItem(R.id.nav_userWithdrawal).setVisible(flag_withdraw);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // 로그인시 보임
        if (LoginPreference.getValue(getApplicationContext(), "id") != null) {
            ((Button) ((NavigationView) findViewById(R.id.nav_view)).getHeaderView(0).findViewById(R.id.login_button)).setText("로그아웃");
            ((NavigationView) findViewById(R.id.nav_view)).getHeaderView(0).findViewById(R.id.register_button).setVisibility(View.GONE);
            if ((Long) LoginPreference.getValue(getApplicationContext(), "shopNo") != -1) { // 사업자 로그인시 보임
                ((NavigationView) findViewById(R.id.nav_view)).getMenu().findItem(R.id.nav_manage).setVisible(true);
            } else { // 일반 사용자 로그인시
                ((NavigationView) findViewById(R.id.nav_view)).getMenu().findItem(R.id.nav_manage).setVisible(false);
            }
        } else if (LoginPreference.getValue(getApplicationContext(), "id") == null) { // 비로그인시
            ((Button) ((NavigationView) findViewById(R.id.nav_view)).getHeaderView(0).findViewById(R.id.login_button)).setText("로그인");
            ((NavigationView) findViewById(R.id.nav_view)).getHeaderView(0).findViewById(R.id.register_button).setVisibility(View.VISIBLE);
            ((NavigationView) findViewById(R.id.nav_view)).getMenu().findItem(R.id.nav_manage).setVisible(false);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onRestart() { // 액티비티가 전면에 보일때
        super.onRestart();
        new MainListAsyncTask().execute(); // 3개가 한세트 (리스트 다시불러오기)
//        mainListArrayAdapter.swap(mainList);
//        mainListArrayAdapter.notifyItemRangeRemoved(0, mainList.size()); // 리스트 갱신을 위한 다 지우기 (리사이클러뷰)
//                mainListArrayAdapter.clear(); // 리스트 갱신을 위한 다 지우기 (리스트뷰)
//        mainListArrayAdapter.notifyDataSetChanged(); // 리스트 갱신을 위한 값 변경시 알려주기?
    }

    /***************************************
     * 생명주기 끝
     ****************************************************/

    // 단순히 액션바부터 네비게이션 돋보기 추가하기 위해서는 여기서 부터~~~
    // 뒤로가기 버튼 누를때... (170123/상욱추가)
    // 네비게이션 드로어가 켜져 있을때 뒤로가기 누르면 드로어가 꺼지고 드로어가 켜져 있지 않을때 뒤로가기 누르면 진짜로 뒤로 감 (170124/상욱추가)
    @Override
    public void onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
//            super.onBackPressed();
            backPressCloseHandler.onBackPressed();
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.login_button:
                if (((Button) findViewById(R.id.login_button)).getText() == "로그인") {
                    Intent intent = new Intent(this, LoginActivity.class);
                    startActivity(intent);
                    break;
                } else if (((Button) findViewById(R.id.login_button)).getText() == "로그아웃") {
                    Toast.makeText(this, LoginPreference.getValue(getApplicationContext(), "id") + " 로그아웃", Toast.LENGTH_SHORT).show();
                    LoginManager.getInstance().logOut();
                    LoginPreference.removeAll(getApplicationContext());
                    onPause();
                    onResume();
                    new MainListAsyncTask().execute(); // 로그아웃 후 즐겨찾기 표시 없애기 위해서 리스트를 다시 갱신시켜줌...
//                    mainListArrayAdapter.swap(mainList);
//                mainListArrayAdapter.notifyItemRangeRemoved(0, mainList.size()); // 리스트 갱신을 위한 다 지우기 (리사이클러뷰)
//                mainListArrayAdapter.clear(); // 리스트 갱신을 위한 다 지우기 (리스트뷰)
//                mainListArrayAdapter.notifyDataSetChanged(); // 리스트 갱신을 위한 값 변경시 알려주기?
                    break;
                }
            case R.id.register_button:
                Intent intent1 = new Intent(this, JoinActivity.class);
                startActivity(intent1);
                break;
            case R.id.logo_id: // 로고 클릭시 새로고침 후 맨 위로!!!
                new MainListAsyncTask().execute();
                recyclerView.smoothScrollToPosition(0);
                break;
        }

        drawer.closeDrawer(GravityCompat.START);
    }

    // 액션바 맨 오른쪽 돋보기 추가 (170123/상욱추가)
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.include_map, menu);
        return true;
    }

    // 돋보기 눌렀을때 효과... (170123/상욱추가)
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // 이곳에 돋보기 눌렀을때 검색액티비티로 이동할 코드 구현하시오!!!
        if (item.getItemId() == R.id.action_button) {
            Intent intent = new Intent(this, SearchActivity.class);
            startActivity(intent);
        } else if (item.getItemId() == R.id.map_button) {
            Intent intent = new Intent(this, SearchMapRangeActivity.class);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }

    // 네비게이션 메뉴 선택시 일어날 작업을 입력하시오 (170123/상욱추가)
    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_myPage) { // 마이페이지
            if ((Long) LoginPreference.getValue(getApplicationContext(), "managerIdentified") == -1) {       // 로그인 안했을때
                Toast.makeText(getApplicationContext(), "로그인하세요", Toast.LENGTH_SHORT).show();
            } else if ((Long) LoginPreference.getValue(getApplicationContext(), "managerIdentified") == 1) { // 사용자 로그인 했을때
                Toast.makeText(getApplicationContext(), "사용자 페이지", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(this, UserMyPageActivity.class);
                startActivity(intent);
            } else if ((Long) LoginPreference.getValue(getApplicationContext(), "managerIdentified") == 2) { // 사업자 로그인 했을때
                Toast.makeText(getApplicationContext(), "사업자 페이지", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(this, OwnerMyPageActivity.class);
                startActivity(intent);
            } else if ((Long) LoginPreference.getValue(getApplicationContext(), "managerIdentified") == 3 || (Long) LoginPreference.getValue(getApplicationContext(), "managerIdentified") == 4) { // 페이지북(3), 구글(4) 로그인 했을때
                Toast.makeText(getApplicationContext(), "소셜로그인중...", Toast.LENGTH_SHORT).show();
            }
        } else if (id == R.id.nav_bookmark) { // 즐겨찾기
            if ((Long) LoginPreference.getValue(getApplicationContext(), "managerIdentified") == -1) {       // 로그인 안했을때
                Toast.makeText(getApplicationContext(), "로그인하세요", Toast.LENGTH_SHORT).show();
            } else {
                Intent intent = new Intent(this, BookmarkActivity.class);
                startActivity(intent);
            }
        } else if (id == R.id.nav_notice) { // 공지사항
            Intent intent = new Intent(this, NoticeActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_setup) { // 알림 설정
            if ((Long) LoginPreference.getValue(getApplicationContext(), "managerIdentified") == -1) {       // 로그인 안했을때
                Toast.makeText(getApplicationContext(), "로그인하세요", Toast.LENGTH_SHORT).show();
            } else {
                Intent intent = new Intent(this, AlarmActivity.class);
                startActivity(intent);
            }
        } else if (id == R.id.nav_help) { // 고객센터
            if ((Long) LoginPreference.getValue(getApplicationContext(), "managerIdentified") == -1) {       // 로그인 안했을때
                Toast.makeText(getApplicationContext(), "로그인하세요", Toast.LENGTH_SHORT).show();
            } else {
                Intent intent = new Intent(this, HelpActivity.class);
                startActivity(intent);
            }
        } else if (id == nav_manage) { // 상품관리
            Intent intent = new Intent(this, ItemActivity.class);
            intent.putExtra("shopNo", (Long) LoginPreference.getValue(getApplicationContext(), "shopNo"));
            startActivity(intent);
        } else if (id == R.id.nav_userWithdrawal) { // 회원탈퇴
            Intent intent = new Intent(this, JoinLeaveActivity.class);
            startActivity(intent);
        }

        drawer.closeDrawer(GravityCompat.START);

        return true;
    }

    public void gps() {
        // checkSelfPermission이 권한 확인
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // requestPermissions가 권한 요청(안드로이드 6.0이상부터 대화상자가 표시된다고 함...)
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{/*android.*/Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
        }
        // 둘다 쓰면 둘중 호출되어 들어온값 사용
        // GPS로 위치정보 얻기
        lm.requestLocationUpdates(GPS_PROVIDER, // 등록할 위치제공자
                1000, // 통지사이의 최소 시간간격 (miliSecond)
                3, // 통지사이의 최소 변경거리 (m)
                locationListener);
        // network로 위치정보 얻기
        lm.requestLocationUpdates(NETWORK_PROVIDER, // 등록할 위치제공자
                1000, // 통지사이의 최소 시간간격 (miliSecond)
                3, // 통지사이의 최소 변경거리 (m)
                locationListener);
    }
}
