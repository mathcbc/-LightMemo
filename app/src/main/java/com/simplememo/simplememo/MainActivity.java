package com.simplememo.simplememo;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

//    private String[] typeArray = {"Apple","Banana","Orange","Watermelon","Pear"};
    private ArrayList<String> typeArray = new ArrayList<>();
    private ArrayList<String> memoArray = new ArrayList<>();
    private ArrayList<TypeItem> typeItemArrayList = new ArrayList<>();
    public static int selectedTypeItemNum = 0;      //默认第一个被选中

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Gson gson = new GsonBuilder().serializeNulls().create();
        //读取typeArray文件并转化为ArrayList<String>类
        final String typeArrayFileName = "typeArray";
        String in = loadStringList(typeArrayFileName);
        typeArray = gson.fromJson(in, new TypeToken<List<String>>(){}.getType());


        //读取typeItemArrayList文件并转化为ArrayList<TypeItem>类
        final String typeItemListFileName = "typeItemArrayList";
        in = loadObject(typeItemListFileName);
        typeItemArrayList = gson.fromJson(in,new TypeToken<List<TypeItem>>(){}.getType());

        //将第一个类目的备忘录列表赋给memoArray
        if (typeItemArrayList!=null&&typeItemArrayList.size()>0){
            try {
                memoArray = (ArrayList<String>) deepCopy(typeItemArrayList.get(selectedTypeItemNum).getMemoList());
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        //create adapter for listViews respectively
        final TypeItemAdapter left_adapter = new TypeItemAdapter(
                MainActivity.this,R.layout.type_item,typeArray);
        final MemoItemAdapter right_adapter = new MemoItemAdapter(
                MainActivity.this,R.layout.memo_item, memoArray);

        //create listView
        final ListView left_listView = (ListView) findViewById(R.id.left_list_view);
        ListView right_listView = (ListView) findViewById(R.id.right_list_view);
        left_listView.setAdapter(left_adapter);
        right_listView.setAdapter(right_adapter);

        //create button
//        Button left_button = (Button)findViewById(R.id.left_button);
//        final Button right_button = (Button)findViewById(R.id.right_button);
//        if (typeArray.size()<2) {
//            right_button.setVisibility(View.INVISIBLE);
//        }

        left_listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //如果按下了最后一个“加号”item
                if (position == left_adapter.getCount() - 1) {
                    //create dialog for users to input category's name
                    AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
                    dialog.setTitle("请输入类目名称");
                    final EditText typeEditText = new EditText(MainActivity.this);
                    typeEditText.setHint("请输入类目名称");
                    dialog.setView(typeEditText);
                    dialog.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            //PositiveButton event
                            String typeName = typeEditText.getText().toString();
                            if (typeName != null && !typeName.isEmpty()) {
                                typeArray.add(typeArray.size()-1,typeName);
                                //typeArray长度等于2时，恢复右按钮的显示
//                                if (typeArray != null && typeArray.size() == 2) {
//                                    right_button.setVisibility(View.VISIBLE);
//                                }
                                typeItemArrayList.add(typeItemArrayList.size()-1,new TypeItem(typeName));
                                typeItemArrayList.get(typeItemArrayList.size() - 2).getMemoList().add("+");
                                selectedTypeItemNum = typeItemArrayList.size() - 2;
                                memoArray.clear();
                                memoArray.addAll(typeItemArrayList.get(selectedTypeItemNum).getMemoList());
                                left_adapter.notifyDataSetChanged();
                                right_adapter.notifyDataSetChanged();
                                //更新typeArray文件和typeItemArrayList文件
                                save(gson.toJson(typeArray), typeArrayFileName);
                                save(gson.toJson(typeItemArrayList), typeItemListFileName);
                            }
                        }
                    });
                    dialog.setNegativeButton("取消", null);
                    dialog.show();
                } else {
                    //按下left_listView的item后，right_listView显示相应TypeItem的memoList
                    selectedTypeItemNum = position;      //记录被选中的类目的位置
                    memoArray.clear();
                    memoArray.addAll(typeItemArrayList.get(position).getMemoList());
                    right_adapter.notifyDataSetChanged();
                    left_adapter.notifyDataSetChanged();
                }
            }

        });

        //长按listView的item出现对话框，对话框含有删除和修改选项
        left_listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
                if (position != left_adapter.getCount() - 1) {
                    final String[] dialogItems = {"删除", "修改", "取消"};
                    AlertDialog.Builder longClickDialog = new AlertDialog.Builder(MainActivity.this);
                    longClickDialog.setAdapter(new ArrayAdapter<>(MainActivity.this, R.layout.diag_item, R.id.dialog_item_name, dialogItems),
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    //点击删除
                                    if (dialogItems[which].equals("删除")) {
                                        AlertDialog.Builder deleteDialog = new AlertDialog.Builder(MainActivity.this);
                                        deleteDialog.setTitle("注意");
                                        deleteDialog.setMessage("该类目下的所有记录将被删除.");
                                        deleteDialog.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                typeArray.remove(position);
                                                typeItemArrayList.remove(position);
                                                memoArray.clear();
                                                memoArray.addAll(typeItemArrayList.get(0).getMemoList());
                                                left_adapter.notifyDataSetChanged();
                                                right_adapter.notifyDataSetChanged();
                                                //更新数据文件
                                                save(gson.toJson(typeArray), typeArrayFileName);
                                                save(gson.toJson(typeItemArrayList), typeItemListFileName);
                                            }
                                        });
                                        deleteDialog.setNegativeButton("取消", null);
                                        deleteDialog.show();
                                    }
                                    //点击修改
                                    if (dialogItems[which].equals("修改")) {
                                        AlertDialog.Builder changeDialog = new AlertDialog.Builder(MainActivity.this);
                                        changeDialog.setTitle("修改类目名称");
                                        final EditText changeEditText = new EditText(MainActivity.this);
                                        changeEditText.setHint("请输入类目名称");
                                        changeDialog.setView(changeEditText);
                                        changeDialog.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                final String changedTypeName = changeEditText.getText().toString();
                                                typeArray.set(position, changedTypeName);
                                                typeItemArrayList.get(position).setTypename(changedTypeName);
                                                save(gson.toJson(typeArray), typeArrayFileName);
                                                left_adapter.notifyDataSetChanged();
                                            }
                                        });
                                        changeDialog.setNegativeButton("取消", null);
                                        AlertDialog tempchangeDiag = changeDialog.create();
                                        tempchangeDiag.setCanceledOnTouchOutside(true);
                                        tempchangeDiag.show();
                                    }
                                }
                            });
                    AlertDialog tempLongClickDialog = longClickDialog.create();
                    tempLongClickDialog.setCanceledOnTouchOutside(true);
                    tempLongClickDialog.show();
                }
                return true;
            }
        });

        right_listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //如果按下了最后一个“加号”item
                if (position ==memoArray.size() - 1) {
                    //create dialog for users to input information
                    AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
                    dialog.setTitle("请输入内容");
                    final EditText editText = new EditText(MainActivity.this);
                    editText.setHint("请输入内容");
                    dialog.setView(editText);
                    dialog.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            //positiveButton event
                            String memo = editText.getText().toString();
                            if (memo != null && !memo.isEmpty()) {
                                memoArray.add(memoArray.size()-1,memo);
                                //往被选中类目的记录列表中添加记录
                                typeItemArrayList.get(selectedTypeItemNum).getMemoList()
                                        .add(typeItemArrayList.get(selectedTypeItemNum).getMemoList().size()-1,memo);
                                save(gson.toJson(typeItemArrayList),typeItemListFileName);
                            }
                        }
                    });
                    dialog.setNegativeButton("取消", null);
                    dialog.show();
                }
            }

        });

        right_listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
                if (position!=memoArray.size()-1){
                    final String[] dialogItems = {"删除", "修改","取消"};
                    AlertDialog.Builder longClickDialog = new AlertDialog.Builder(MainActivity.this);
                    longClickDialog.setAdapter(new ArrayAdapter<>(MainActivity.this, R.layout.diag_item, R.id.dialog_item_name, dialogItems),
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    //点击删除
                                    if (dialogItems[which].equals("删除")) {
                                        memoArray.remove(position);
                                        typeItemArrayList.get(selectedTypeItemNum).getMemoList().remove(position);
                                        save(gson.toJson(typeItemArrayList), typeItemListFileName);
                                        right_adapter.notifyDataSetChanged();
                                    }
                                    //点击修改
                                    if (dialogItems[which].equals("修改")) {
                                        AlertDialog.Builder changeDialog = new AlertDialog.Builder(MainActivity.this);
                                        changeDialog.setTitle("修改内容");
                                        final EditText changeEditText = new EditText(MainActivity.this);
                                        changeEditText.setHint("请输入内容");
                                        changeDialog.setView(changeEditText);
                                        changeDialog.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                final String changedMemo = changeEditText.getText().toString();
                                                if (!changedMemo.isEmpty()) {
                                                    typeItemArrayList.get(selectedTypeItemNum).getMemoList().set(position, changedMemo);
                                                    memoArray.clear();
                                                    memoArray.addAll(typeItemArrayList.get(selectedTypeItemNum).getMemoList());
                                                    save(gson.toJson(typeItemArrayList), typeItemListFileName);
                                                    right_adapter.notifyDataSetChanged();
                                                }
                                            }
                                        });
                                        changeDialog.setNegativeButton("取消", null);
                                        changeDialog.show();
                                    }
                                }
                            });
                    AlertDialog tempLongClickDialog = longClickDialog.create();
                    tempLongClickDialog.setCanceledOnTouchOutside(true);
                    tempLongClickDialog.show();
                }
                return true;
            }
        });


        //left button set on ClickListener
//        left_button.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                //create dialog for users to input category's name
//                AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
//                dialog.setTitle("请输入类目名称");
//                final EditText typeEditText = new EditText(MainActivity.this);
//                typeEditText.setHint("请输入类目名称");
//                dialog.setView(typeEditText);
//                dialog.setPositiveButton("确定", new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//                        //PositiveButton event
//                        String typeName = typeEditText.getText().toString();
//                        if (typeName != null && !typeName.isEmpty()) {
//                            typeArray.add(typeArray.size()-1,typeName);
//                            //typeArray长度等于2时，恢复右按钮的显示
//                            if (typeArray != null && typeArray.size() == 2) {
//                                right_button.setVisibility(View.VISIBLE);
//                            }
//                            typeItemArrayList.add(typeItemArrayList.size()-1,new TypeItem(typeName));
//                            typeItemArrayList.get(typeItemArrayList.size() - 2).getMemoList().add("+");
//                            selectedTypeItemNum = typeItemArrayList.size() - 2;
//                            memoArray.clear();
//                            memoArray.addAll(typeItemArrayList.get(selectedTypeItemNum).getMemoList());
//                            left_adapter.notifyDataSetChanged();
//                            right_adapter.notifyDataSetChanged();
//                            //更新typeArray文件和typeItemArrayList文件
//                            save(gson.toJson(typeArray), typeArrayFileName);
//                            save(gson.toJson(typeItemArrayList), typeItemListFileName);
//                        }
//                    }
//                });
//                dialog.setNegativeButton("取消", null);
//                dialog.show();
//            }
//        });

//        right button set on ClickListener
//        right_button.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                //create dialog for users to input information
//                AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
//                dialog.setTitle("请输入内容");
//                final EditText editText = new EditText(MainActivity.this);
//                editText.setHint("请输入内容");
//                dialog.setView(editText);
//                dialog.setPositiveButton("确定", new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//                        //positiveButton event
//                        String memo = editText.getText().toString();
//                        if (memo != null && !memo.isEmpty()) {
//                            memoArray.add(memoArray.size()-1,memo);
//                            //往被选中类目的记录列表中添加记录
//                            typeItemArrayList.get(selectedTypeItemNum).getMemoList()
//                                    .add(typeItemArrayList.get(selectedTypeItemNum).getMemoList().size() - 1, memo);
//                            save(gson.toJson(typeItemArrayList),typeItemListFileName);
//                        }
//                    }
//                });
//                dialog.setNegativeButton("取消", null);
//                dialog.show();
//            }
//        });
//
    }

    public void save(String json,String fileName) {
        FileOutputStream fout;
        BufferedWriter writer = null;
        try {
            fout = openFileOutput(fileName, MODE_PRIVATE);
            writer = new BufferedWriter(new OutputStreamWriter(fout));
            writer.write(json);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            try{
                if (writer != null){
                    writer.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public String load(String fileName){
        FileInputStream in;
        BufferedReader reader = null;
        StringBuilder content = new StringBuilder();
        try{
            in = openFileInput(fileName);
            reader = new BufferedReader(new InputStreamReader(in));
            String line;
            while((line = reader.readLine())!=null){
                content.append(line);
            }
        } catch (FileNotFoundException e) {     //若文件不存在，则新建一个空文件
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if (reader!=null){
                try{
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return content.toString();
    }

    public String loadStringList(String fileName){
        FileInputStream in;
        BufferedReader reader = null;
        StringBuilder content = new StringBuilder();
        try{
            in = openFileInput(fileName);
            reader = new BufferedReader(new InputStreamReader(in));
            String line;
            while((line = reader.readLine())!=null){
                content.append(line);
            }
        } catch (FileNotFoundException e) {     //若文件不存在，则新建一个空文件
            save("[\"+\"]", fileName);
            return loadStringList(fileName);
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if (reader!=null){
                try{
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return content.toString();
    }

    public String loadObject(String fileName){
        FileInputStream in;
        BufferedReader reader = null;
        StringBuilder content = new StringBuilder();
        Gson gson = new Gson();
        ArrayList<TypeItem> typeItemArrayList = new ArrayList<>();
        typeItemArrayList.add(new TypeItem("+"));
        try{
            in = openFileInput(fileName);
            reader = new BufferedReader(new InputStreamReader(in));
            String line;
            while((line = reader.readLine())!=null){
                content.append(line);
            }
        } catch (FileNotFoundException e) {     //若文件不存在，则新建一个空文件
            save(gson.toJson(typeItemArrayList), fileName);
            return load(fileName);
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if (reader!=null){
                try{
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return content.toString();
    }

    public static <T> List<T> deepCopy(List<T> src) throws IOException, ClassNotFoundException {
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(byteOut);
        out.writeObject(src);

        ByteArrayInputStream byteIn = new ByteArrayInputStream(byteOut.toByteArray());
        ObjectInputStream in = new ObjectInputStream(byteIn);
        @SuppressWarnings("unchecked")
        List<T> dest = (List<T>) in.readObject();
        return dest;
    }
}
