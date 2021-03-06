package content.eml

import grails.converters.JSON

import org.grails.taggable.Tag

import java.io.File;
import java.io.InputStream;
import java.util.List

import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.springframework.web.multipart.MultipartHttpServletRequest
import org.codehaus.groovy.grails.plugins.springsecurity.SpringSecurityUtils;
import grails.converters.JSON
import static org.codehaus.groovy.grails.commons.ConfigurationHolder.config as Config
import org.springframework.http.HttpStatus
import uk.co.desirableobjects.ajaxuploader.exception.FileUploadException
import org.springframework.web.multipart.MultipartHttpServletRequest
import org.springframework.web.multipart.commons.CommonsMultipartFile
import org.springframework.web.multipart.MultipartFile
import javax.servlet.http.HttpServletRequest
import uk.co.desirableobjects.ajaxuploader.AjaxUploaderService
import grails.util.GrailsNameUtils
import grails.plugins.springsecurity.Secured


import speciespage.ObservationService
import species.utils.Utils
import content.eml.Document
import content.eml.Document.DocumentType
import content.eml.UFile;

class UFileController {

	static allowedMethods = [save: "POST", update: "POST", delete: "POST"]

	def observationService
	def springSecurityService;
	def grailsApplication

	def config = org.codehaus.groovy.grails.commons.ConfigurationHolder.config

	String contentRootDir = config.speciesPortal.content.rootDir


	AjaxUploaderService ajaxUploaderService
	UFileService uFileService = new UFileService()

	def index = {
		redirect(action: "list", params: params)
	}



	def browser = {
		log.debug params

		def model = getUFileList(params)
		render (view:"browser", model:model)
		return;
	}


	/**
	 *  For uploading a file.
	 *  File is uploaded to a temporary location. No UFile object is created in this method
	 *  params takes the relative path. if not given, uploads to default content root directory
	 */

	@Secured(['ROLE_USER'])
	def fileUpload = {
		log.debug params
		try {

			//IE handling: for IE qqfile sends the whole file
			String originalFilename = ""
			if (params.qqfile instanceof org.springframework.web.multipart.commons.CommonsMultipartFile){
				log.debug "Multipart"
				//content = params.qqfile.getBytes()
				originalFilename = params.qqfile.originalFilename
			}
			else{
				log.debug "normal"
				//content = request.inputStream.getBytes()
				originalFilename = params.qqfile
			}
			File uploaded = createFile(originalFilename, params.uploadDir)
			InputStream inputStream = selectInputStream(request)

			ajaxUploaderService.upload(inputStream, uploaded)


			String relPath = uploaded.absolutePath.replace(contentRootDir, "")

			//def url = uGroup.createLink(uri:uploaded.getPath() , 'userGroup':params.userGroupInstance, 'userGroupWebaddress':params.webaddress)
			def url = g.createLinkTo(base:config.speciesPortal.content.serverURL, file: relPath)
			//log.debug "url for uploaded file >>>>>>>>>>>>>>>>>>>>>>>>"+ url

			return render(text: [success:true, filePath:relPath, fileURL: url, fileSize:UFileService.getFileSize(uploaded)] as JSON, contentType:'text/html')
		} catch (FileUploadException e) {

			log.error("Failed to upload file.", e)
			return render(text: [success:false] as JSON, contentType:'text/html')
		}
	}

	/**
	 * upload of file in project.
	 * Document is created after uploading of file. THe document id is passed to form and for further tracking.
	 */
	@Secured(['ROLE_USER'])
	def upload = {
		log.debug "&&&&&&&&&&&&&&&&&&& <><><<>>params in upload of file" +  params
		try {

			//IE handling: for IE qqfile sends the whole file
			String originalFilename = ""
			if (params.qqfile instanceof org.springframework.web.multipart.commons.CommonsMultipartFile){
				log.debug "Multipart"
				//content = params.qqfile.getBytes()
				originalFilename = params.qqfile.originalFilename
			}
			else{
				log.debug "normal"
				//content = request.inputStream.getBytes()
				originalFilename = params.qqfile
			}
			File uploaded = createFile(originalFilename, params.uploadDir)
			InputStream inputStream = selectInputStream(request)
			//check for file size and file type

			ajaxUploaderService.upload(inputStream, uploaded)


			String relPath = uploaded.absolutePath.replace(contentRootDir, "")

			UFile uFileInstance = new UFile()
			uFileInstance.path = relPath
			uFileInstance.size = UFileService.getFileSize(uploaded)
			uFileInstance.downloads = 0

			Document documentInstance = new Document()
			documentInstance.title  = uploaded.getName()

			if(params.type) {
				switch(params.type) {
					case "Proposal":
						documentInstance.type = DocumentType.Proposal
						break
					case "Report":
						documentInstance.type = DocumentType.Report
						break
					case "Poster":
						documentInstance.type = DocumentType.Poster
						break
					case "Miscellaneous":
					default:
						documentInstance.type = DocumentType.Miscellaneous
						break
				}
			} else {
				documentInstance.type = DocumentType.Miscellaneous
			}
			documentInstance.author = springSecurityService.currentUser

			documentInstance.uFile = uFileInstance


			documentInstance.save(flush:true)


			log.debug " parameters to projectDoc block >>>> Path - "+ uFileInstance.path + " ,  Id: "+ documentInstance.id + ", fileSize:"+uFileInstance.size+", docName:"+documentInstance.title

			return render(text: [success:true, filePath:relPath, docId:documentInstance.id, fileSize:uFileInstance.size, docName:documentInstance.title] as JSON, contentType:'text/html')
		} catch (FileUploadException e) {

			log.error("Failed to upload file.", e)
			return render(text: [success:false] as JSON, contentType:'text/html')
		}
	}

	private InputStream selectInputStream(HttpServletRequest request) {
		if (request instanceof MultipartHttpServletRequest) {
			MultipartFile uploadedFile = ((MultipartHttpServletRequest) request).getFile('qqfile')
			return uploadedFile.inputStream
		}
		return request.inputStream
	}

	//Create file with given filename
	private File createFile(String fileName, String uploadDir) {
		File uploaded
		if (uploadDir) {
			File fileDir = new File(contentRootDir + "/"+ uploadDir)
			if(!fileDir.exists())
				fileDir.mkdirs()
			uploaded = observationService.getUniqueFile(fileDir, Utils.generateSafeFileName(fileName));

		} else {

			File fileDir = new File(contentRootDir)
			if(!fileDir.exists())
				fileDir.mkdirs()
			uploaded = observationService.getUniqueFile(fileDir, Utils.generateSafeFileName(fileName));
			//uploaded = File.createTempFile('grails', 'ajaxupload')
		}
		
		log.debug "New file created : "+ uploaded.getPath()
		return uploaded
	}


	def download = {

		UFile ufile = UFile.get(params.id)
		if (!ufile) {
			def msg = messageSource.getMessage("fileupload.download.nofile", [params.id] as Object[], request.locale)
			log.debug msg
			flash.message = msg
			redirect controller: params.errorController, action: params.errorAction
			return
		}

		def file = new File(ufile.path)
		if (file.exists()) {
			log.debug "Serving file id=[${ufile.id}] for the ${ufile.downloads} to ${request.remoteAddr}"
			ufile.downloads++
			ufile.save()
			response.setContentType("application/octet-stream")
			response.setHeader("Content-disposition", "${params.contentDisposition}; filename=${file.name}")
			response.outputStream << file.readBytes()
			return
		} else {
			def msg = messageSource.getMessage("fileupload.download.filenotfound", [ufile.name] as Object[], request.locale)
			log.error msg
			flash.message = msg
			redirect controller: params.errorController, action: params.errorAction
			return
		}
	}

	protected def getUFileList(params) {

		def max = Math.min(params.max ? params.int('max') : 12, 100)
		def offset = params.offset ? params.int('offset') : 0
		def filteredUFile = uFileService.getFilteredUFiles(params, max, offset)
		def UFileInstanceList = filteredUFile.UFileInstanceList
		def queryParams = filteredUFile.queryParams
		def activeFilters = filteredUFile.activeFilters

		def totalUFileInstanceList = uFileService.getFilteredUFiles(params, -1, -1).UFileInstanceList
		def count = totalUFileInstanceList.size()

		return [totalUFileInstanceList:totalUFileInstanceList, UFileInstanceList: UFileInstanceList, UFileInstanceTotal: count, queryParams: queryParams, activeFilters:activeFilters, total:count]

	}



}
