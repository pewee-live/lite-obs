package com.pewee.bean.dto;

import com.pewee.bean.LogicFile;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.apache.http.util.Asserts;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;

/**
 * sysAlias sysCode filename mimeType length crc32 split 均为必填参数
 * 如果split传了1 那么total必填
 *
 * @author GongRan
 */
@Builder
@AllArgsConstructor
@ToString
@Getter
public class PreUploadDto implements Serializable {

    private static final long serialVersionUID = -3812640799050797543L;

    private PreUploadDto() {

    }

    /**
     * 文件名  file_name
     */
    private String fileName;

    /**
     * mime类型  mime_type
     */
    private String mimeType;

    /**
     * 长度  length
     */
    private Long length;

    /**
     * 存储类型 (0: nas)  storage_type
     */
    private Integer storageType;

    /**
     * 是否切片 (0:不切片 1:切片)  split
     */
    private Integer split;

    /**
     * 分片数  total
     */
    private Integer total;

    /**
     * 校验和  crc32
     */
    private String crc32;

    /**
     * 系统编码  sys_code
     */
    private String sysCode;

    /**
     * 系统代号  sys_alisa
     */
    private String sysAlisa;

    /**
     * sysAlias sysCode filename mimeType length crc32 split 均为必填参数
     * *如果split传了1 那么total必填
     */
    private void valid() {
        Asserts.notBlank(sysAlisa, "sysAlias不能为空!");
        Asserts.notBlank(sysCode, "sysCode不能为空!");
        Asserts.notBlank(fileName, "filename不能为空!");
        Asserts.notBlank(mimeType, "mimeType不能为空!");
        Asserts.notNull(length, "length不能为null!");
        Asserts.notBlank(crc32, "crc32不能为空!");
        Asserts.notNull(split, "split不能为null!");
        if (split < 0 || split > 1) {
            throw new IllegalArgumentException("split非法!");
        }
        boolean isSplit = split.equals(1);
        boolean isTotalNullOrZero = (total == null || total == 0);
        if ( isSplit && isTotalNullOrZero) {
            throw new IllegalStateException("在分片前提下,total不合法!");
        }
    }

    public LogicFile generateLogicFile(){
        LogicFile logicFile = new LogicFile();
        BeanUtils.copyProperties(this,logicFile);
        if (this.split.equals(0)){
            logicFile.setTotal(1);
        }
        return logicFile;
    }


    public static class OuterPreUploadDto extends PreUploadDtoBuilder {
        public OuterPreUploadDto() {
            super();
        }

        @Override
        public PreUploadDto build() {
            PreUploadDto model = super.build();
            model.valid();
            return model;
        }
    }

    public static PreUploadDtoBuilder builder() {
        return new OuterPreUploadDto();
    }
}

