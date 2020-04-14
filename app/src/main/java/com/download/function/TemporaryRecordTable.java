package com.download.function;

import com.download.db.DataBaseHelper;
import com.download.entity.DownloadType;
import com.download.entity.TemporaryRecord;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import retrofit2.Response;

import static com.download.function.Constant.DOWNLOAD_RECORD_FILE_DAMAGED;
import static com.download.function.Utils.contentLength;
import static com.download.function.Utils.empty;
import static com.download.function.Utils.fileName;
import static com.download.function.Utils.lastModify;
import static com.download.function.Utils.log;
import static com.download.function.Utils.notSupportRange;

public class TemporaryRecordTable {
    private Map<String, TemporaryRecord> map;

    public TemporaryRecordTable() {
        this.map = new HashMap<>();
    }

    public void add(String url, TemporaryRecord record) {
        this.map.put(url, record);
    }

    public boolean contain(String url) {
        return this.map.get(url) != null;
    }

    public void delete(String url) {
        this.map.remove(url);
    }

    /**
     * Save file info
     *
     * @param url      key
     * @param response response
     */
    public void saveFileInfo(String url, Response<?> response) {
        TemporaryRecord record = this.map.get(url);

        if (empty(record.getSaveName())) {
            record.setSaveName(fileName(url, response));
        }

        record.setContentLength(contentLength(response));
        record.setLastModify(lastModify(response));
    }

    /**
     * Save range info
     *
     * @param url      key
     * @param response response
     */
    public void saveRangeInfo(String url, Response<?> response) {
        this.map.get(url).setRangeSupport(!notSupportRange(response));
    }

    /**
     * Init necessary info
     *
     * @param url             url
     * @param maxThreads      max threads
     * @param maxRetryCount   retry count
     * @param defaultSavePath default save path
     * @param downloadApi     api
     * @param dataBaseHelper  DataBaseHelper
     */
    public void init(String url, int maxThreads, int maxRetryCount, String defaultSavePath, DownloadApi downloadApi, DataBaseHelper dataBaseHelper) {
        this.map.get(url).init(maxThreads, maxRetryCount, defaultSavePath, downloadApi, dataBaseHelper);
    }

    /**
     * Save file state, change or not change.
     *
     * @param url      key
     * @param response response
     */
    public void saveFileState(String url, Response<Void> response) {
        if (response.code() == 304) {
            this.map.get(url).setFileChanged(false);
        } else if (response.code() == 200) {
            this.map.get(url).setFileChanged(true);
        }
    }

    /**
     * return file not exists download type.
     *
     * @param url key
     * @return download type
     */
    public DownloadType generateNonExistsType(String url) {
        return getNormalType(url);
    }

    /**
     * return file exists download type
     *
     * @param url key
     * @return download type
     */
    public DownloadType generateFileExistsType(String url) {
        DownloadType type;
        if (fileChanged(url)) {
            type = getNormalType(url);
        } else {
            type = getServerFileChangeType(url);
        }
        return type;
    }

    /**
     * read last modify string
     *
     * @param url key
     * @return last modify
     */
    public String readLastModify(String url) {
        try {
            return this.map.get(url).readLastModify();
        } catch (IOException e) {
            //TODO log
            //If read failed,return an empty string.
            //If we send empty last-modify,server will response 200.
            //That means file changed.
            return "";
        }
    }

    public boolean fileExists(String url) {
        return this.map.get(url).file().exists();
    }

    public File[] getFiles(String url) {
        return this.map.get(url).getFiles();
    }

    private boolean supportRange(String url) {
        return this.map.get(url).isSupportRange();
    }

    private boolean fileChanged(String url) {
        return this.map.get(url).isFileChanged();
    }

    private DownloadType getNormalType(String url) {
        DownloadType type;
        if (this.supportRange(url)) {
            type = new DownloadType.MultiThreadDownload(this.map.get(url));
        } else {
            type = new DownloadType.NormalDownload(this.map.get(url));
        }
        return type;
    }

    private DownloadType getServerFileChangeType(String url) {
        if (this.supportRange(url)) {
            return this.supportRangeType(url);
        } else {
            return this.notSupportRangeType(url);
        }
    }

    private DownloadType supportRangeType(String url) {
        if (this.needReDownload(url)) {
            return new DownloadType.MultiThreadDownload(this.map.get(url));
        }
        try {
            if (this.multiDownloadNotComplete(url)) {
                return new DownloadType.ContinueDownload(this.map.get(url));
            }
        } catch (IOException e) {
            return new DownloadType.MultiThreadDownload(this.map.get(url));
        }
        return new DownloadType.AlreadyDownloaded(this.map.get(url));
    }

    private DownloadType notSupportRangeType(String url) {
        if (this.normalDownloadNotComplete(url)) {
            return new DownloadType.NormalDownload(this.map.get(url));
        } else {
            return new DownloadType.AlreadyDownloaded(this.map.get(url));
        }
    }

    private boolean multiDownloadNotComplete(String url) throws IOException {
        return this.map.get(url).fileNotComplete();
    }

    private boolean normalDownloadNotComplete(String url) {
        return !this.map.get(url).fileComplete();
    }

    private boolean needReDownload(String url) {
        return this.tempFileNotExists(url) || this.tempFileDamaged(url);
    }

    private boolean tempFileDamaged(String url) {
        try {
            return this.map.get(url).tempFileDamaged();
        } catch (IOException e) {
            log(DOWNLOAD_RECORD_FILE_DAMAGED);
            return true;
        }
    }

    private boolean tempFileNotExists(String url) {
        return !this.map.get(url).tempFile().exists();
    }
}
