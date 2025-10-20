package com.pewee.bean.bdpan;

import java.util.List;
import java.util.Map;

import lombok.Data;

/**
 * 查询百度盘文件dto
 * @author pewee
 *
 */
@Data
public class BdQueryFileDto {
	
	private Integer errno;
	
	private String errmsg;
	
	private List<Map<String,String>> list;
	
	/**
	 * {
	"errmsg": "succ",
	"errno": 0,
	"list": [
		{
			"category": 6,
			"dlink": "https://d.pcs.baidu.com/file/4cb81f119ica34f7ef2f0ab4307dcc5b?fid=4005009946-250528-399742917312296&rt=pr&sign=FDtAERV-DCb740ccc5511e5e8fedcff06b081203-KTvBDuP9a9bMFDRb2tnA14dFxng%3D&expires=8h&chkbd=0&chkv=3&dp-logid=2101336084509017416&dp-callid=0&dstime=1684979626&r=499135968&origin_appid=27128669&file_type=0",
			"filename": "692809913088782336",
			"fs_id": 399742917312296,
			"isdir": 0,
			"local_ctime": 1684919613,
			"local_mtime": 1684919613,
			"md5": "4cb81f119ica34f7ef2f0ab4307dcc5b",
			"oper_id": 4005009946,
			"path": "/obs/system_no_00003/2023-05-24/692809913088782336",
			"server_ctime": 1684919613,
			"server_mtime": 1684919613,
			"size": 16777216
		},
		{
			"category": 6,
			"dlink": "https://d.pcs.baidu.com/file/c64714607sc6fb0dfb908939fbd8a977?fid=4005009946-250528-190866204777466&rt=pr&sign=FDtAERV-DCb740ccc5511e5e8fedcff06b081203-vHD1jy8AXvXZLiMYQGOaX6L5eIU%3D&expires=8h&chkbd=0&chkv=3&dp-logid=2101336158792704454&dp-callid=0&dstime=1684979626&r=499135968&origin_appid=27128669&file_type=0",
			"filename": "692810074493988864",
			"fs_id": 190866204777466,
			"isdir": 0,
			"local_ctime": 1684919652,
			"local_mtime": 1684919652,
			"md5": "c64714607sc6fb0dfb908939fbd8a977",
			"oper_id": 4005009946,
			"path": "/obs/system_no_00003/2023-05-24/692810074493988864",
			"server_ctime": 1684919652,
			"server_mtime": 1684919652,
			"size": 16777216
		},
		{
			"category": 6,
			"dlink": "https://d.pcs.baidu.com/file/cc849bd21l801c2f3b0504a87b32fe59?fid=4005009946-250528-1115642505521555&rt=pr&sign=FDtAERV-DCb740ccc5511e5e8fedcff06b081203-HV2SwLqeVgUsx7EXRMTAeLVySX4%3D&expires=8h&chkbd=0&chkv=3&dp-logid=2101336210894702590&dp-callid=0&dstime=1684979626&r=499135968&origin_appid=27128669&file_type=0",
			"filename": "692810158732390400",
			"fs_id": 1115642505521555,
			"isdir": 0,
			"local_ctime": 1684919672,
			"local_mtime": 1684919672,
			"md5": "cc849bd21l801c2f3b0504a87b32fe59",
			"oper_id": 4005009946,
			"path": "/obs/system_no_00003/2023-05-24/692810158732390400",
			"server_ctime": 1684919672,
			"server_mtime": 1684919672,
			"size": 16777216
		},
		{
			"category": 6,
			"dlink": "https://d.pcs.baidu.com/file/f8150d1dfj778a9a3c713152636d61fd?fid=4005009946-250528-864557852381257&rt=pr&sign=FDtAERV-DCb740ccc5511e5e8fedcff06b081203-Ixls1ziZ3wrnyaDSy5ZNOIy%2FBRM%3D&expires=8h&chkbd=0&chkv=3&dp-logid=2101336263255377741&dp-callid=0&dstime=1684979626&r=499135968&origin_appid=27128669&file_type=0",
			"filename": "692810236490592256",
			"fs_id": 864557852381257,
			"isdir": 0,
			"local_ctime": 1684919689,
			"local_mtime": 1684919689,
			"md5": "f8150d1dfj778a9a3c713152636d61fd",
			"oper_id": 4005009946,
			"path": "/obs/system_no_00003/2023-05-24/692810236490592256",
			"server_ctime": 1684919689,
			"server_mtime": 1684919689,
			"size": 3410486
		}
	],
	"names": {},
	"request_id": "9113937486069521064"
}
	 */
	
}
