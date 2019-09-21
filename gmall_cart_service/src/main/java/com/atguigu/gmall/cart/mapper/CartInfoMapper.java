package com.atguigu.gmall.cart.mapper;

import com.atguigu.bean.CartInfo;
import org.apache.ibatis.annotations.Param;
import org.springframework.web.bind.annotation.RequestParam;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;


public interface CartInfoMapper extends Mapper<CartInfo> {

    public List<CartInfo> selectCartListWithSkuPrice(String userId);

    public void mergeCartList(@Param("userIdDest")String userIdDest, @Param("userIdOrig") String userIdOrig);
}
