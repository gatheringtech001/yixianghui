package com.ruoyi.system.service.impl;

import java.util.*;

import com.ruoyi.common.core.domain.entity.SysDept;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.bean.BeanValidators;
import com.ruoyi.system.domain.AppConsultant;
import com.ruoyi.system.domain.CustomerStatic;
import com.ruoyi.system.service.IAppConsultantService;
import com.ruoyi.system.service.ISysDeptService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.AppCustomerMapper;
import com.ruoyi.system.domain.AppCustomer;
import com.ruoyi.system.service.IAppCustomerService;

import javax.validation.Validator;

/**
 * 客户资料Service业务层处理
 * 
 * @author lankong
 * @date 2025-05-07
 */
@Service
public class AppCustomerServiceImpl implements IAppCustomerService 
{
    private static final Logger log = LoggerFactory.getLogger(AppCustomerServiceImpl.class);

    @Autowired
    private AppCustomerMapper appCustomerMapper;

    @Autowired
    private ISysDeptService deptService;

    @Autowired
    protected Validator validator;

    @Autowired
    private IAppConsultantService consultantService;

    /**
     * 查询客户资料
     * 
     * @param customerId 客户资料主键
     * @return 客户资料
     */
    @Override
    public AppCustomer selectAppCustomerByCustomerId(Long customerId)
    {
        return appCustomerMapper.selectAppCustomerByCustomerId(customerId);
    }

    /**
     * 查询客户资料列表
     * 
     * @param appCustomer 客户资料
     * @return 客户资料
     */
    @Override
    public List<AppCustomer> selectAppCustomerList(AppCustomer appCustomer)
    {
        return appCustomerMapper.selectAppCustomerList(appCustomer);
    }

    /**
     * 新增客户资料
     * 
     * @param appCustomer 客户资料
     * @return 结果
     */
    @Override
    public int insertAppCustomer(AppCustomer appCustomer)
    {
        appCustomer.setCreateTime(DateUtils.getNowDate());
        return appCustomerMapper.insertAppCustomer(appCustomer);
    }

    /**
     * 修改客户资料
     * 
     * @param appCustomer 客户资料
     * @return 结果
     */
    @Override
    public int updateAppCustomer(AppCustomer appCustomer)
    {
        appCustomer.setUpdateTime(DateUtils.getNowDate());
        return appCustomerMapper.updateAppCustomer(appCustomer);
    }

    /**
     * 批量删除客户资料
     * 
     * @param customerIds 需要删除的客户资料主键
     * @return 结果
     */
    @Override
    public int deleteAppCustomerByCustomerIds(Long[] customerIds)
    {
        return appCustomerMapper.deleteAppCustomerByCustomerIds(customerIds);
    }

    /**
     * 删除客户资料信息
     * 
     * @param customerId 客户资料主键
     * @return 结果
     */
    @Override
    public int deleteAppCustomerByCustomerId(Long customerId)
    {
        return appCustomerMapper.deleteAppCustomerByCustomerId(customerId);
    }



    /**
     * 导入客户资料
     * @param customerList
     * @param isUpdateSupport
     * @param operName
     * @return
     */
    @Override
    public String importCustomer(List<AppCustomer> customerList, boolean isUpdateSupport, String operName) {
        if (StringUtils.isNull(customerList) || customerList.size() == 0)
        {
            throw new ServiceException("导入客户数据不能为空！");
        }
        int successNum = 0;
        int failureNum = 0;
        StringBuilder successMsg = new StringBuilder();
        StringBuilder failureMsg = new StringBuilder();
        for (AppCustomer customer : customerList)
        {
            try
            {
                //匹配关联ID-顾问姓名
                AppConsultant consultant = consultantService.selectAppConsultantByConsultantName(customer.getConsultantName());
                if (consultant != null) {
                    customer.setConsultantId(consultant.getConsultantId());
                }
                SysDept dept = deptService.selectDeptByName(customer.getDeptName());
                if (dept != null) {
                    customer.setDeptId(dept.getDeptId());
                }
                // 验证是否存在这个客户
                AppCustomer u = appCustomerMapper.selectCustomerByCustomerName(customer.getCustomerName());
                if (StringUtils.isNull(u))
                {
                    BeanValidators.validateWithException(validator, customer);
                    customer.setCreateBy(operName);
                    appCustomerMapper.insertAppCustomer(customer);
                    successNum++;
                    successMsg.append("<br/>" + successNum + "、客户 " + customer.getCustomerName() + " 导入成功");
                }
                else if (isUpdateSupport)
                {
                    BeanValidators.validateWithException(validator, customer);
                    deptService.checkDeptDataScope(customer.getDeptId());
                    AppCustomer lastCustomer = appCustomerMapper.selectCustomerByCustomerName(u.getCustomerName());
                    customer.setCustomerId(lastCustomer.getCustomerId());
                    customer.setUpdateBy(operName);
                    appCustomerMapper.updateAppCustomer(customer);
                    successNum++;
                    successMsg.append("<br/>" + successNum + "、客户 " + customer.getCustomerName() + " 更新成功");
                }
                else
                {
                    failureNum++;
                    failureMsg.append("<br/>" + failureNum + "、客户 " + customer.getCustomerName() + " 已存在");
                }
            }
            catch (Exception e)
            {
                failureNum++;
                String msg = "<br/>" + failureNum + "、客户 " + customer.getCustomerName() + " 导入失败：";
                failureMsg.append(msg + e.getMessage());
                log.error(msg, e);
            }
        }
        if (failureNum > 0)
        {
            failureMsg.insert(0, "很抱歉，导入失败！共 " + failureNum + " 条数据格式不正确，错误如下：");
            throw new ServiceException(failureMsg.toString());
        }
        else
        {
            successMsg.insert(0, "恭喜您，数据已全部导入成功！共 " + successNum + " 条，数据如下：");
        }
        return successMsg.toString();
    }

    /**
     * 通过客户姓名查询客户
     * @param customerName
     * @return
     */
    @Override
    public AppCustomer selectCustomerByCustomerName(String customerName) {
        return appCustomerMapper.selectCustomerByCustomerName(customerName);
    }

    /**
     * 根据用户ID查询客户信息
     * @param userId
     * @return
     */
    @Override
    public AppCustomer selectAppCustomerByUserId(Long userId) {
        return appCustomerMapper.selectAppCustomerByUserId(userId);
    }

    @Override
    public int saveLiveAddressByUserId(Long userId, String liveAddress, String customerName, String linkMobile)
    {
        if (userId == null)
        {
            return 0;
        }
        AppCustomer customer = appCustomerMapper.selectAppCustomerByUserId(userId);
        if (customer == null)
        {
            if (StringUtils.isEmpty(liveAddress))
            {
                return 0;
            }
            customer = new AppCustomer();
            customer.setUserId(userId);
            customer.setCustomerName(StringUtils.defaultString(customerName));
            customer.setLinkMobile(linkMobile);
            customer.setLiveAddress(liveAddress);
            return insertAppCustomer(customer);
        }
        customer.setLiveAddress(liveAddress);
        return updateAppCustomer(customer);
    }

    @Override
    public Map custStatic() {
        Map retmap = new HashMap();
        try{
            List<CustomerStatic> custGenderStatic = appCustomerMapper.custGenderStatic();
            List<CustomerStatic> custGoodsStatic = appCustomerMapper.custGoodsStatic();
            List<CustomerStatic> custNumStatic = appCustomerMapper.custNumStatic();
            List<CustomerStatic> custInsureEvaStatic = appCustomerMapper.custInsureEvaStatic();
            List<CustomerStatic> custResidentialStatic = appCustomerMapper.custResidentialStatic();
            int totalcount = 0;
            if(null!=custGenderStatic){
                int malecount = 0;
                int femalecount = 0;
                int unknowncount = 0;
                for(CustomerStatic customerStatic:custGenderStatic){
                    if("男".equals(StringUtils.defaultString(customerStatic.getGroupColOne()))){
                        malecount = malecount + customerStatic.getDataCount();
                    }else if("女".equals(StringUtils.defaultString(customerStatic.getGroupColOne()))){
                        femalecount = femalecount + customerStatic.getDataCount();
                    }else{
                        unknowncount = unknowncount + customerStatic.getDataCount();
                    }
                }
                Map malemap = new HashMap();
                malemap.put("value",malecount);
                malemap.put("name","男");
                malemap.put("itemStyle","{ color: '#ec8e04' }");
                Map femalemap = new HashMap();
                femalemap.put("value",femalecount);
                femalemap.put("name","女");
                femalemap.put("itemStyle","{ color: '#d58004' }");
                Map unknownmap = new HashMap();
                unknownmap.put("value",unknowncount);
                unknownmap.put("name","空值");
                unknownmap.put("itemStyle","{ color: '#E6A23C' }");
                List<Map> genderlist = new ArrayList<>();
                genderlist.add(malemap);
                genderlist.add(femalemap);
                genderlist.add(unknownmap);
                retmap.put("genderStatic",genderlist);
                totalcount = malecount+femalecount+unknowncount;
                retmap.put("totalcount",totalcount);
            }
            if(null!=custGoodsStatic){
                Collections.sort(custGoodsStatic,(o1,o2)->{
                    return o2.getDataCount()-o1.getDataCount();
                });
                int[] ydataarr = new int[custGoodsStatic.size()];
                String[] xdataarr = new String[custGoodsStatic.size()];
                int avalcou = 0;
                for(int i=0;i<custGoodsStatic.size();i++){
                    if(custGoodsStatic.get(i).getDataCount()>0) {
                        avalcou = avalcou + 1;
                    }
                    ydataarr[i] = custGoodsStatic.get(i).getDataCount();
                    xdataarr[i] = custGoodsStatic.get(i).getGroupColOne();
                }
                int precou = 20;
                if(avalcou < 20){
                    precou = avalcou;
                }
                int[] ydataarrnew = new int[precou];
                String[] xdataarrnew = new String[precou];
                int newind = 0;
                for(int i=0;i<ydataarr.length;i++){
                    if(ydataarr[i]>0){
                        ydataarrnew[newind] = ydataarr[i];
                        xdataarrnew[newind] = xdataarr[i];
                        newind = newind + 1;
                    }
                    if(newind==precou){
                        break;
                    }
                }
                Map goodsmap = new HashMap();
                goodsmap.put("ydata",ydataarrnew);
                goodsmap.put("xdata",xdataarrnew);
                retmap.put("goodsStatic",goodsmap);
            }
            if(null!=custResidentialStatic){
                Collections.sort(custResidentialStatic,(o1,o2)->{
                    return o1.getDataCount()-o2.getDataCount();
                });
                int[] ydataarr = new int[custResidentialStatic.size()];
                String[] xdataarr = new String[custResidentialStatic.size()];
                for(int i=0;i<custResidentialStatic.size();i++){
                    ydataarr[i] = custResidentialStatic.get(i).getDataCount();
                    xdataarr[i] = custResidentialStatic.get(i).getGroupColOne();
                }
                Map residentialmap = new HashMap();
                residentialmap.put("ydata",ydataarr);
                residentialmap.put("xdata",xdataarr);
                retmap.put("residentialStatic",residentialmap);
            }
            if(null!=custNumStatic){
                int[] ydataarr = new int[custNumStatic.size()];
                String[] xdataarr = new String[custNumStatic.size()];
                for(int i=0;i<custNumStatic.size();i++){
                    ydataarr[i] = custNumStatic.get(i).getDataCount();
                    if(null==custNumStatic.get(i).getGroupColOne()) {
                        xdataarr[i] = "空值";
                    }else{
                        xdataarr[i] = custNumStatic.get(i).getGroupColOne();
                    }
                }
                Map nummap = new HashMap();
                nummap.put("ydata",ydataarr);
                nummap.put("xdata",xdataarr);
                retmap.put("numStatic",nummap);
            }
            if(null!=custInsureEvaStatic){
                Map lengendmap = new HashMap();
                int legendcount = 0;
                List<String> lengendlist = new ArrayList();
                Map lengendXData = new HashMap();
                Map xkeysmap = new HashMap();
                for(int i=0;i<custInsureEvaStatic.size();i++){
                    if(null!=lengendmap.get(custInsureEvaStatic.get(i).getGroupColTwo())){
                        lengendXData = (Map) lengendmap.get(custInsureEvaStatic.get(i).getGroupColTwo());
                        lengendXData.put(custInsureEvaStatic.get(i).getGroupColOne(),custInsureEvaStatic.get(i).getDataCount());
                        lengendmap.put(custInsureEvaStatic.get(i).getGroupColTwo(),lengendXData);
                        if(lengendXData.keySet().size()>legendcount){
                            legendcount = lengendXData.keySet().size();
                        }
                    }else{
                        lengendlist.add(custInsureEvaStatic.get(i).getGroupColTwo());
                        lengendXData = new HashMap();
                        lengendXData.put(custInsureEvaStatic.get(i).getGroupColOne(),custInsureEvaStatic.get(i).getDataCount());
                        lengendmap.put(custInsureEvaStatic.get(i).getGroupColTwo(),lengendXData);
                        if(legendcount==0){
                            legendcount =  1;
                        }
                    }
                    xkeysmap.put(custInsureEvaStatic.get(i).getGroupColOne(),"1");
                }
                String[] legendarr = new String[lengendlist.size()];
                for(int i=0;i<lengendlist.size();i++){
                    legendarr[i] = lengendlist.get(i);
                }
                Iterator it = xkeysmap.keySet().iterator();
                String[] xkeysarr = new String[xkeysmap.keySet().size()];
                String tmpkeystr;
                int keyind = 0;
                Map ydatamap;
                int[] ydataarr;
                Object tmpobj = null;
                while(it.hasNext()){
                    tmpobj = it.next();
                    if(null!=tmpobj){
                        tmpkeystr = tmpobj.toString();
                    }else{
                        tmpkeystr = "空值";
                    }
                    xkeysarr[keyind++] = tmpkeystr;
                }
                List ydatalist = new ArrayList();
                for(int i=0;i<lengendlist.size();i++){
                    ydatamap = new HashMap();
                    ydatamap.put("name",lengendlist.get(i));
                    ydatamap.put("type","bar");
                    ydatamap.put("barWidth","3");
                    if(i%lengendlist.size()==0) {
                        ydatamap.put("itemStyle", "{\n" +
                                "            color: '#044a9a'\n" +
                                "          }");
                    }else if(i%4==1) {
                        ydatamap.put("itemStyle", "{\n" +
                                "            color: '#c57704'\n" +
                                "          }");
                    }else if(i%4==2) {
                        ydatamap.put("itemStyle", "{\n" +
                                "            color: '#04dbe1'\n" +
                                "          }");
                    }else if(i%4==3) {
                        ydatamap.put("itemStyle", "{\n" +
                                "            color: '#5bdd05'\n" +
                                "          }");
                    }
                    lengendXData = (Map) lengendmap.get(lengendlist.get(i));
                    ydataarr = new int[lengendlist.size()];
                    for(int j=0;j<xkeysarr.length;j++){
                        if(null!=lengendXData.get(xkeysarr[j])){
                            ydataarr[i] = (int) lengendXData.get(xkeysarr[j]);
                        }else{
                            ydataarr[i] = 0;
                        }
                    }
                    ydatamap.put("data",ydataarr);
                    ydatalist.add(ydatamap);
                }
                Map insureEvaStatic = new HashMap();
                insureEvaStatic.put("insureEvaStaticData",ydatalist);
                insureEvaStatic.put("insureEvaStaticLegend",legendarr);
                insureEvaStatic.put("insureEvaStaticXKeys",xkeysarr);
                retmap.put("insureEvaStatic",insureEvaStatic);
            }
        }catch (Exception ex){
            ex.printStackTrace();
        }
        return retmap;
    }
}
