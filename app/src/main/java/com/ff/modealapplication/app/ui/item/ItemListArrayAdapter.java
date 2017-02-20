package com.ff.modealapplication.app.ui.item;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Paint;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.ff.modealapplication.R;
import com.ff.modealapplication.andorid.network.SafeAsyncTask;
import com.ff.modealapplication.app.core.service.ItemService;
import com.ff.modealapplication.app.core.util.Base;
import com.ff.modealapplication.app.ui.message.MessagingService;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

import java.util.List;
import java.util.Map;

/**
 * Created by bit-desktop on 2017-02-01.
 */

public class ItemListArrayAdapter extends ArrayAdapter<Map<String, Object>> {

    private Long no;
    ItemService itemService = new ItemService();
    Context context;


    // 상품 목록에 기본 이미지 출력
    private LayoutInflater layoutInflater;
    DisplayImageOptions displayImageOption = new DisplayImageOptions.Builder()
            .showImageForEmptyUri(R.drawable.apple)
            .showImageOnFail(R.drawable.apple)
            .delayBeforeLoading(0)
            .cacheOnDisc(true)
            .build();

    // 체크된 아이템들을 저장할 List
    boolean[] isChecked;

    public ItemListArrayAdapter(Context context) {
        super(context, R.layout.item_list);
        this.context = context;
        layoutInflater = LayoutInflater.from(context);
    }

    @NonNull
    @Override
    public View getView(final int position, View convertView, ViewGroup parent) { // 내부 많이 변경 (170207/상욱변경)
        View view = convertView;
        final Map<String, Object> map = getItem(position);
        if (convertView == null) {
            convertView = layoutInflater.inflate(R.layout.item_list_row, parent, false);
        }

        ((TextView) convertView.findViewById(R.id.item_list_clock)).setText(map.get("expDate").toString());        // 유통기한
        ((TextView) convertView.findViewById(R.id.item_list_name)).setText(map.get("name").toString());            // 상품명
//        textOriPriceView.setPaintFlags(textOriPriceView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);         //가운데 줄긋기
        ((TextView) convertView.findViewById(R.id.item_list_ori_price)).setText((map.get("oriPrice")).toString());   // 원가
        ((TextView) convertView.findViewById(R.id.item_list_price)).setText(map.get("price").toString());          // 판매가

        // 액티비티로 데이터 보내기 위해서...
        ((TextView) convertView.findViewById(R.id.send_no)).setText(String.valueOf(((Double) map.get("no")).longValue()));

        // 상품 이미지
        ImageLoader.getInstance().init(ImageLoaderConfiguration.createDefault(getContext()));
        ImageLoader.getInstance().displayImage(Base.url + "modeal/shop/images/" + map.get("picture"),
                (ImageView) convertView.findViewById(R.id.item_list_image), displayImageOption);                // 상품이미지

        // 보이기/숨기기 버튼 클릭시
        final ToggleButton hideButton = (ToggleButton) convertView.findViewById(R.id.item_list_button_hiding);
//        hideButton.setChecked(false);
//        if (((Double) map.get("showItem")).longValue() == 1L) {
//            isChecked[position] = false;
//        } else {
//            isChecked[position] = true;
//        }
        if (isChecked[position] || ((Double) map.get("showItem")).longValue() == 0L) {
            hideButton.setChecked(true);
            isChecked[position] = true;
        } else if (!isChecked[position] || ((Double) map.get("showItem")).longValue() == 1L){
            hideButton.setChecked(false);
            isChecked[position] = false;
        }
        hideButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (hideButton.isChecked()) {
                    isChecked[position] = true;
                    new ItemView(((Double) map.get("no")).longValue(), 0L).execute();
                } else {
                    isChecked[position] = false;
                    new ItemView(((Double) map.get("no")).longValue(), 1L).execute();
                    // 보이기 누르면 해당 알림 구독한 사용자들에게 알림 전송
                    new Thread() {
                        public void run() {
                            MessagingService.send(map.get("name") + " 상품이 등록되었습니다.", // 제목
                                    map.get("shopName") + " 매장의 " + map.get("name") + " 상품이 등록되었습니다.", // 내용
                                    "bi" + ((Double) map.get("no")).longValue()); // 알림 번호
                        }
                    }.start();
                }
            }
        });

         // 수정 버튼 클릭시 -----------------------------------------------------------------------
        convertView.findViewById(R.id.item_list_button_modify).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getContext(), ItemModifyActivity.class);
//                intent.putExtra("no", ((TextView) v.findViewById(R.id.send_no)).getText().toString());
//                intent.putExtra("no", (TextView) v.findViewById(R.id.send_no)).getLongExtra("no", -1L);
                getContext().startActivity(intent);
            }
        });

        // 삭제 버튼 클릭시 ------------------------------------------------------------------------
        convertView.findViewById(R.id.item_list_button_delete).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(final View view) {

                new AlertDialog.Builder(getContext()).
                        setTitle("삭제").
                        setIcon(R.drawable.delete).
                        setMessage("해당 상품을\n삭제하시겠습니까?\n").

                        setPositiveButton("예", new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                new ItemDelete(((Double) map.get("no")).longValue()).execute();
                                Log.d("setPositiveButton", "" + which);
                            }
                        }).

                        setNegativeButton("아니요", new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        }).
                        show();
            }
        });
        return convertView;
    }

    // 상품 보이기 / 숨기기
    private class ItemView extends SafeAsyncTask<Void> {
        private Long check;
        private Long no;

        public ItemView(Long no, Long check) {
            this.check = check;
            this.no = no;
        }

        @Override
        public Void call() throws Exception {
            itemService.itemView(no, check);
            return null;
        }

        @Override
        protected void onException(Exception e) throws RuntimeException {
            Log.d("*View Exception error :", "" + e);
            throw new RuntimeException(e);
        }
    }

    // 삭제되는 동안 다른 작업이 실행되기 위해서 사용
    private class ItemDelete extends SafeAsyncTask<Void> {
        Long no;

        public ItemDelete(Long no) {
            this.no = no;
        }

        @Override
        public Void call() throws Exception {
            itemService.itemDelete(no);
            return null;
        }

        @Override // 에러나면 Exception 발생
        protected void onException(Exception e) throws RuntimeException {
            Log.d("!!!!!!!!!!!!!", "" + e);
            super.onException(e);
        }

        @Override // 성공하면 해당 상품이 삭제된 상품목록 출력
        protected void onSuccess(Void Void) throws Exception {
            Toast.makeText(getContext(), "해당 상품이 삭제되었습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    // 목록에 상품이 추가됨 ------------------------------------------------------------------------
    public void add(List<Map<String, Object>> list) {
        if (list == null) {
            return;
        }
        isChecked = new boolean[list.size()];
        for (Map<String, Object> map : list) {
            Log.d("test", "" + map);
            add(map);
        }
    }
}
