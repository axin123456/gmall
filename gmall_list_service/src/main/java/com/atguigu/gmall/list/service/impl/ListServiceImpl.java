package com.atguigu.gmall.list.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.bean.SkuLsInfo;
import com.atguigu.bean.SkuLsParams;
import com.atguigu.bean.SkuLsResult;
import com.atguigu.service.ListService;
import io.searchbox.client.JestClient;
import io.searchbox.core.Index;
import io.searchbox.core.Search;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.TermsBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.List;

@Service
public class ListServiceImpl implements ListService {

    @Autowired
    JestClient jestClient;
    public void  saveSkuLsInfo(SkuLsInfo skuLsInfo){
        Index.Builder builder = new Index.Builder(skuLsInfo);
        builder.index("gmall_sku_info").type("doc").id(skuLsInfo.getId());
        Index index = builder.build();
        try {
            jestClient.execute(index);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public SkuLsResult getSkuLsInfoList(SkuLsParams skuLsParams) {

        String query="{\n" +
                "  \"query\": {\n" +
                "    \"bool\": {\n" +
                "      \"must\": [\n" +
                "        {\"match\": {\n" +
                "          \"skuName\": \""+skuLsParams.getKeyword()+"\"\n" +
                "        }}\n" +
                "      ],\n" +
                "      \"filter\": [ \n" +
                "          {\"term\": {\n" +
                "            \"catalog3Id\": \"61\"\n" +
                "          }},\n" +
                "          {\"term\": {\n" +
                "            \"skuAttrValueList.valueId\": \"83\"\n" +
                "          }},\n" +
                "          {\"term\": {\n" +
                "            \"skuAttrValueList.valueId\": \"154\"\n" +
                "          }},\n" +
                "          \n" +
                "           {\"range\": {\n" +
                "            \"price\": {\"gte\": 3200}\n" +
                "           }}\n" +
                "\n" +
                "        ]\n" +
                "      }\n" +
                "    },\n" +
                "    \"from\": 0\n" +
                "    , \"size\": 2\n" +
                "    , \"highlight\": {\"fields\": {\"skuName\": {\"pre_tags\": \"<span style='color:red' >\",\"post_tags\": \"</span>\"}}  }\n" +
                "  \n" +
                "    ,\n" +
                "    \"aggs\": {\n" +
                "      \"groupby_valueid\": {\n" +
                "        \"terms\": {\n" +
                "          \"field\": \"skuAttrValueList.valueId\",\n" +
                "          \"size\": 1000\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"sort\": [\n" +
                "      {\n" +
                "        \"hotScore\": {\n" +
                "          \"order\": \"desc\"\n" +
                "        }\n" +
                "      }\n" +
                "    ]\n" +
                "}";

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
        boolQueryBuilder.must(new MatchQueryBuilder("skuName",skuLsParams.getKeyword()));
        boolQueryBuilder.filter(new TermQueryBuilder("catalog3Id",skuLsParams.getCatalog3Id()));
        String[] valueIds = skuLsParams.getValueId();

        for (int i = 0; i < valueIds.length; i++) {
            String valueid = valueIds[i];
            boolQueryBuilder.filter(new TermQueryBuilder("skuAttrValueList.valueId",valueid));
        }

        searchSourceBuilder.query(boolQueryBuilder);
        searchSourceBuilder.from(skuLsParams.getPageNo()-1*skuLsParams.getPageSize());
        searchSourceBuilder.size(skuLsParams.getPageSize());
        //高亮
        searchSourceBuilder.highlight(new HighlightBuilder().field("skuName").preTags("<span style='color:red'>" ).postTags("</span>"));
       //聚合
        TermsBuilder termsBuilder = AggregationBuilders.terms("groupby_value_id").field("skuAttrValueList.valueId").size(1000);
        searchSourceBuilder.aggregation(termsBuilder);
        //排序
        searchSourceBuilder.sort("hotSoure", SortOrder.DESC);

        System.out.println(searchSourceBuilder.toString());

        Search.Builder searchBuilder = new Search.Builder(searchSourceBuilder.toString());
        Search search = searchBuilder.addIndex("gmall_sku_info").addType("doc").build();
        try {
            jestClient.execute(search);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
