/**
 * Copyright (c) 2015-2016, Michael Yang 杨福海 (fuhai999@gmail.com).
 *
 * Licensed under the GNU Lesser General Public License (LGPL) ,Version 3.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.gnu.org/licenses/lgpl-3.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.jpress.controller.admin;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Date;
import java.util.List;

import com.jfinal.aop.Before;
import com.jfinal.kit.PathKit;
import com.jfinal.log.Log;
import com.jfinal.upload.UploadFile;

import io.jpress.Consts;
import io.jpress.core.JBaseCRUDController;
import io.jpress.core.Jpress;
import io.jpress.core.annotation.UrlMapping;
import io.jpress.interceptor.ActionCacheClearInterceptor;
import io.jpress.model.Attachment;
import io.jpress.model.Option;
import io.jpress.model.User;
import io.jpress.template.Thumbnail;
import io.jpress.utils.AttachmentUtils;
import io.jpress.utils.FileUtils;
import io.jpress.utils.ImageUtils;

@UrlMapping(url = "/admin/attachment", viewPath = "/WEB-INF/admin/attachment")
@Before(ActionCacheClearInterceptor.class)
public class _AttachmentController extends JBaseCRUDController<Attachment> {
	private static final Log log = Log.getLog(_AttachmentController.class);

	public void detail_layer() {
		BigInteger id = getParaToBigInteger("id");
		Attachment attachment = Attachment.DAO.findById(id);
		setAttr("attachment", attachment);
	}

	public void choose_layer() {
		index();
		render("choose_layer.html");
	}

	public void upload() {
		keepPara();
	}

	public void doUpload() {
		UploadFile uploadFile = getFile();
		if (null != uploadFile) {
			String newPath = AttachmentUtils.moveFile(uploadFile);
			User user = getAttr(Consts.ATTR_USER);

			Attachment attachment = new Attachment();
			attachment.setUserId(user.getId());
			attachment.setCreated(new Date());
			attachment.setTitle("");
			attachment.setPath(newPath);
			attachment.setSuffix(FileUtils.getSuffix(uploadFile.getFileName()));
			attachment.setMimeType(uploadFile.getContentType());
			attachment.save();
			
			processImage(newPath);
			
			//{"success":true}
			renderJson("success", true);
//			redirect("/admin/attachment?p=attachment&c=list", true);
		} else {
			renderJson("success", false);
//			redirect("/admin/attachment/upload?p=attachment&c=upload", true);
		}
	}

	private void processImage(String newPath) {
		if(!AttachmentUtils.isImage(newPath))
			return;
		
		try {
			//由于内存不够等原因可能会出未知问题
			processThumbnail(newPath);
		} catch (Throwable e) {
			log.error("processThumbnail error",e);
		}
		try {
			//由于内存不够等原因可能会出未知问题
			processWatermark(newPath);
		} catch (Throwable e) {
			log.error("processWatermark error",e);
		}
	}

	private void processThumbnail(String newPath) {
		List<Thumbnail> tbs = Jpress.currentTemplate().getThumbnails();
		if (tbs != null && tbs.size() > 0) {
			for (Thumbnail tb : tbs) {
				try {
					String newSrc = ImageUtils.scale(PathKit.getWebRootPath() + newPath, tb.getWidth(), tb.getHeight());
					processWatermark(FileUtils.removeRootPath(newSrc));
				} catch (IOException e) {
					log.error("processWatermark error", e);
				}
			}
		}
	}

	public void processWatermark(String newPath) {
		Boolean watermark_enable = Option.findValueAsBool("watermark_enable");
		if (watermark_enable != null && watermark_enable) {

			int position = Option.findValueAsInteger("watermark_position");
			String watermarkImg = Option.findValue("watermark_image");
			String srcImageFile = newPath;

			Float transparency = Option.findValueAsFloat("watermark_transparency");
			if (transparency == null || transparency < 0 || transparency > 1) {
				transparency = 1f;
			}

			watermarkImg = PathKit.getWebRootPath() + watermarkImg;
			srcImageFile = PathKit.getWebRootPath() + srcImageFile;

			ImageUtils.pressImage(watermarkImg, srcImageFile, srcImageFile, position, transparency);
		}
	}

}
