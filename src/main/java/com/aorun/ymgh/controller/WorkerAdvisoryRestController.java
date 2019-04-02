package com.aorun.ymgh.controller;


import com.aorun.ymgh.controller.login.UserDto;
import com.aorun.ymgh.model.WorkerMember;
import com.aorun.ymgh.dto.WorkerAdvisoryDto;
import com.aorun.ymgh.model.WorkerAdvisory;
import com.aorun.ymgh.service.WorkerAdvisoryService;
import com.aorun.ymgh.util.CheckObjectIsNull;
import com.aorun.ymgh.util.DateFriendlyShow;
import com.aorun.ymgh.util.PageConstant;
import com.aorun.ymgh.util.biz.ImagePropertiesConfig;
import com.aorun.ymgh.util.biz.UnionUtil;
import com.aorun.ymgh.util.cache.redis.RedisCache;
import com.aorun.ymgh.util.jsonp.Jsonp;
import com.aorun.ymgh.util.jsonp.Jsonp_data;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 *我的咨询&留言
 * Created by bysocket on 07/02/2017.
 */
@RequestMapping("/worker")
@RestController
public class WorkerAdvisoryRestController {

    @Autowired
    private WorkerAdvisoryService workerAdvisoryService;

    /**
     *
     * @param sid
     * @param advisoryBizType   1-咨询，2-留言
     * @param pageIndex
     * @param pageSize
     * @return
     */
        //1.我的咨询----列表接口----分页查询
        @RequestMapping(value = "/workerAdvisoryList", method = RequestMethod.GET)
        public Object workerLiveClaimList(
            @RequestParam(name = "sid", required = true, defaultValue = "") String sid,
            @RequestParam(name = "advisoryBizType", required = true, defaultValue = "") Integer advisoryBizType,
            @RequestParam(name="pageIndex", required = true, defaultValue = "1") Integer pageIndex,
            @RequestParam(name="pageSize", required = false, defaultValue = PageConstant.APP_PAGE_SIZE + "") Integer pageSize
            ) {



            UserDto user = null;
            WorkerMember workerMember = null;
            if (!StringUtils.isEmpty(sid)) {
                user = (UserDto) RedisCache.get(sid);
                if (CheckObjectIsNull.isNull(user)) {
                    return Jsonp.noLoginError("请先登录或重新登录");
                }
                workerMember = RedisCache.getObj(UnionUtil.generateUnionSid(user),WorkerMember.class);
                if (CheckObjectIsNull.isNull(workerMember)) {
                    return Jsonp.noAccreditError("用户未授权工会,请重新授权");
                }
            } else {
                return Jsonp.noLoginError("用户SID不正确,请核对后重试");
            }



        Long workerId = workerMember.getId();
        List<WorkerAdvisory>   workerAdvisoryList = new ArrayList<WorkerAdvisory>();
        List<WorkerAdvisoryDto>   workerAdvisoryDtoList = new ArrayList<WorkerAdvisoryDto>();
            workerAdvisoryList = workerAdvisoryService.getWorkerAdvisoryListByWorkerId(workerId,advisoryBizType,pageIndex,pageSize);
        for(WorkerAdvisory workerAdvisory:workerAdvisoryList){
            WorkerAdvisoryDto workerAdvisoryDto = new WorkerAdvisoryDto();
            BeanUtils.copyProperties(workerAdvisory,workerAdvisoryDto);
            BeanUtils.copyProperties(workerAdvisory,workerAdvisoryDto);

            StringBuffer MaterialsUrls = new StringBuffer("");
            String materialsUrls =  workerAdvisory.getMaterialsUrls();
            if(materialsUrls!=null&&!materialsUrls.equals("")){
                String _MaterialsUrls[] = materialsUrls.split(",");
                for(String materialsUrl:_MaterialsUrls){
                    MaterialsUrls.append(ImagePropertiesConfig.APPLY_CARD_SERVER_PATH+materialsUrl).append(",");
                }
            }
            workerAdvisoryDto.setMaterialsUrls(MaterialsUrls.toString());
            workerAdvisoryDto.setCreateTime(DateFriendlyShow.showTimeText(workerAdvisory.getCreateTime()));
            workerAdvisoryDtoList.add(workerAdvisoryDto);
        }


        return Jsonp_data.success(workerAdvisoryDtoList);
    }






    /**
     *
     * @param sid
     * @param advisoryType   咨询类别 1-劳动合同,2-工资福利,3-五险一金,4-休息休假,5-其他
     * @param advisoryBizType   1-咨询，2-留言
     * @param advisoryName
     * @param telephone
     * @param companyName
     * @param advisoryTitle
     * @param advisoryContent
     * @param materialsFiles
     * @return
     */
    //新增接口
    @RequestMapping(value = "/workerAdvisory", method = RequestMethod.POST)
    public Object createWorkerAdvisoryClaim(
            @RequestParam(name = "sid", required = true, defaultValue = "") String sid,
            @RequestParam(name = "advisoryType", required = true, defaultValue = "") Integer advisoryType,
            @RequestParam(name = "advisoryBizType", required = true, defaultValue = "") Integer advisoryBizType,
            @RequestParam(name = "advisoryName", required = true, defaultValue = "") String advisoryName,
            @RequestParam(name = "telephone", required = true, defaultValue = "") String telephone,
            @RequestParam(name = "companyName", required = true, defaultValue = "") String companyName,
            @RequestParam(name = "advisoryTitle", required = true, defaultValue = "") String advisoryTitle,
            @RequestParam(name = "advisoryContent", required = true, defaultValue = "") String advisoryContent,
            @RequestParam("materialsFiles") List<MultipartFile> materialsFiles) {
        UserDto user = null;
        WorkerMember workerMember = null;
        if (!StringUtils.isEmpty(sid)) {
            user = (UserDto) RedisCache.get(sid);
            if (CheckObjectIsNull.isNull(user)) {
                return Jsonp.noLoginError("请先登录或重新登录");
            }
            workerMember = RedisCache.getObj(UnionUtil.generateUnionSid(user),WorkerMember.class);
            if (CheckObjectIsNull.isNull(workerMember)) {
                return Jsonp.noAccreditError("用户未授权工会,请重新授权");
            }
        } else {
            return Jsonp.noLoginError("用户SID不正确,请核对后重试");
        }


        Long workerId = workerMember.getId();
        WorkerAdvisory workerAdvisory = new WorkerAdvisory();
        workerAdvisory.setWorkerId(workerId);
        workerAdvisory.setAdvisoryType(advisoryType);
        workerAdvisory.setAdvisoryBizType(advisoryBizType);
        workerAdvisory.setAdvisoryName(advisoryName);
        workerAdvisory.setTelephone(telephone);
        workerAdvisory.setCompanyName(companyName);
        workerAdvisory.setAdvisoryTitle(advisoryTitle);
        workerAdvisory.setAdvisoryContent(advisoryContent);

        if (materialsFiles==null && materialsFiles.size()<0) {
            return Jsonp.error("文件不能为空!");
        }
        try {
            StringBuffer materialsUrls = new StringBuffer("");
            for(MultipartFile file:materialsFiles){
                // Get the file and save it somewhere
                byte[] bytes = file.getBytes();
                String uuid = UUID.randomUUID().toString();
                String suffixName = file.getOriginalFilename().substring(file.getOriginalFilename().indexOf("."));
                String fileName  = uuid+suffixName;
                Path path = Paths.get(ImagePropertiesConfig.ADVISORY_PATH + fileName);
                Files.write(path, bytes);
                materialsUrls.append(fileName).append(",");
            }
            workerAdvisory.setMaterialsUrls(materialsUrls.toString());

        } catch (IOException e) {
            e.printStackTrace();
        }
        workerAdvisoryService.saveWorkerAdvisory(workerAdvisory);
        return Jsonp.success();
    }



    //修改接口
    @RequestMapping(value = "/workerAdvisory", method = RequestMethod.PUT)
    public Object updateWorkerAdvisoryClaim(@RequestParam(name = "sid", required = true, defaultValue = "") String sid,
                                            @RequestParam(name = "id", required = true) Long id,
                                            @RequestParam(name = "advisoryType", required = true, defaultValue = "") Integer advisoryType,
                                            @RequestParam(name = "advisoryBizType", required = true, defaultValue = "") Integer advisoryBizType,
                                            @RequestParam(name = "advisoryName", required = true, defaultValue = "") String advisoryName,
                                            @RequestParam(name = "telephone", required = true, defaultValue = "") String telephone,
                                            @RequestParam(name = "companyName", required = true, defaultValue = "") String companyName,
                                            @RequestParam(name = "advisoryTitle", required = true, defaultValue = "") String advisoryTitle,
                                            @RequestParam(name = "advisoryContent", required = true, defaultValue = "") String advisoryContent,
                                            @RequestParam("materialsFiles") List<MultipartFile> materialsFiles) {

        UserDto user = null;
        WorkerMember workerMember = null;
        if (!StringUtils.isEmpty(sid)) {
            user = (UserDto) RedisCache.get(sid);
            if (CheckObjectIsNull.isNull(user)) {
                return Jsonp.noLoginError("请先登录或重新登录");
            }
            workerMember = RedisCache.getObj(UnionUtil.generateUnionSid(user),WorkerMember.class);
            if (CheckObjectIsNull.isNull(workerMember)) {
                return Jsonp.noAccreditError("用户未授权工会,请重新授权");
            }
        } else {
            return Jsonp.noLoginError("用户SID不正确,请核对后重试");
        }


        WorkerAdvisory workerAdvisory = workerAdvisoryService.findWorkerAdvisoryById(id);
        if(workerAdvisory!=null){
//            Long workerId = workerMember.getId();
//            workerAdvisory.setWorkerId(workerId);
//            workerAdvisory.setAdvisoryType(advisoryType);
//            workerAdvisory.setAdvisoryBizType(advisoryBizType);
            workerAdvisory.setAdvisoryName(advisoryName);
            workerAdvisory.setTelephone(telephone);
            workerAdvisory.setCompanyName(companyName);
            workerAdvisory.setAdvisoryTitle(advisoryTitle);
            workerAdvisory.setAdvisoryContent(advisoryContent);

            if (materialsFiles==null && materialsFiles.size()<0) {
                return Jsonp.error("文件不能为空!");
            }
            try {
                StringBuffer materialsUrls = new StringBuffer("");
                for(MultipartFile file:materialsFiles){
                    // Get the file and save it somewhere
                    byte[] bytes = file.getBytes();
                    String uuid = UUID.randomUUID().toString();
                    String suffixName = file.getOriginalFilename().substring(file.getOriginalFilename().indexOf("."));
                    String fileName  = uuid+suffixName;
                    Path path = Paths.get(ImagePropertiesConfig.ADVISORY_PATH + fileName);
                    Files.write(path, bytes);
                    materialsUrls.append(fileName).append(",");
                }
                workerAdvisory.setMaterialsUrls(materialsUrls.toString());

            } catch (IOException e) {
                e.printStackTrace();
            }

            workerAdvisory.setStatus(1);
            workerAdvisoryService.updateWorkerAdvisory(workerAdvisory);
            return Jsonp.success();
        }else{
            return Jsonp.bussiness_tips_code("暂无此信息!");
        }

    }


    //详情接口
    @RequestMapping(value = "/workerAdvisory/{id}", method = RequestMethod.GET)
    public Object findOneWorkerAdvisory(@PathVariable("id") Long id) {

        WorkerAdvisory workerAdvisory = workerAdvisoryService.findWorkerAdvisoryById(id);
        WorkerAdvisoryDto workerAdvisoryDto = new WorkerAdvisoryDto();
        BeanUtils.copyProperties(workerAdvisory,workerAdvisoryDto);


        StringBuffer MaterialsUrls = new StringBuffer("");
        String materialsUrls =  workerAdvisory.getMaterialsUrls();
        if(materialsUrls!=null&&!materialsUrls.equals("")){
            String _MaterialsUrls[] = materialsUrls.split(",");
            for(String materialsUrl:_MaterialsUrls){
                MaterialsUrls.append(ImagePropertiesConfig.APPLY_CARD_SERVER_PATH+materialsUrl).append(",");
            }
        }
        workerAdvisoryDto.setMaterialsUrls(MaterialsUrls.toString());
        workerAdvisoryDto.setCreateTime(DateFriendlyShow.showTimeText(workerAdvisory.getCreateTime()));

        return Jsonp_data.success(workerAdvisoryDto);
    }

    //删除接口
    @RequestMapping(value = "/workerAdvisory/{id}", method = RequestMethod.DELETE)
    public Object deleteWorkerAdvisory(@PathVariable("id") Long id) {
        int flag = workerAdvisoryService.deleteWorkerAdvisory(id);
        if(flag>0){
            return Jsonp.success();
        }else{
            return Jsonp.bussiness_tips_code("删除失败");
        }
    }

}