package de.baumann.browser.Bamboo;

public interface OnLoadFinishedListener {
    /** 페이지 로딩이 끝나면 호출**/
    void onLoadFinish(String html);
    /** 이미지 로딩이 끝나면 호출**/
    void onReloadFinish(String html);
}
