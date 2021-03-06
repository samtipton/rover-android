package io.rover.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;

/**
 * Created by ata_n on 2016-06-16.
 */
public class Row implements Parcelable {

    private Unit mHeight;
    private ArrayList<Block> mBlocks;
    private Block mBackgroundBlock;

    public Row(ArrayList<Block> blocks) {

        mBackgroundBlock = new Block();
        mBackgroundBlock.setPosition(Block.Position.Floating);
        mBackgroundBlock.setAlignment(new Alignment(Alignment.Horizontal.Fill, Alignment.Vertical.Fill));

        mBlocks = blocks;

        mBlocks.add(mBackgroundBlock);
    }

    public ArrayList<Block> getBlocks() { return mBlocks; }

    public Unit getHeight() { return mHeight; }

    public void setHeight(Unit height) { mHeight = height; }

    public Block getBackgroundBlock() { return mBackgroundBlock; }

    /** Parcelable
     */

    protected Row(Parcel in) {
        mHeight = (Unit) in.readValue(Unit.class.getClassLoader());
        if (in.readByte() == 0x01) {
            mBlocks = new ArrayList<Block>();
            in.readList(mBlocks, Block.class.getClassLoader());
        } else {
            mBlocks = null;
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeValue(mHeight);
        if (mBlocks == null) {
            dest.writeByte((byte) (0x00));
        } else {
            dest.writeByte((byte) (0x01));
            dest.writeList(mBlocks);
        }
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<Row> CREATOR = new Parcelable.Creator<Row>() {
        @Override
        public Row createFromParcel(Parcel in) {
            return new Row(in);
        }

        @Override
        public Row[] newArray(int size) {
            return new Row[size];
        }
    };
}
