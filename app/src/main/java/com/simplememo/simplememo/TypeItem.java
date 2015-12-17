package com.simplememo.simplememo;

import java.util.ArrayList;

/**
 * Created by Benson on 2015/12/6.
 */
public class TypeItem {
    private String typename;
    private ArrayList<String> memoList = new ArrayList<>();

    public TypeItem(String typename)
    {
        this.typename = typename;
    }

    public TypeItem(String typename, ArrayList<String> memoList )
    {
        this.typename = typename;
        this.memoList = memoList;
    }

    public String getTypename()
    {
        return typename;
    }

    public void setTypename(String typename) {
        this.typename = typename;
    }

    public void addMemo(String memo)
    {
        this.memoList.add(memo);
    }

    public ArrayList<String> getMemoList()
    {
        return memoList;
    }
}
