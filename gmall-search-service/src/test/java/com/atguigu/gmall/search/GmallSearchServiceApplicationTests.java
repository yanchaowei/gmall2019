package com.atguigu.gmall.search;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.PmsSearchSkuInfo;
import com.atguigu.gmall.bean.PmsSkuInfo;
import com.atguigu.gmall.service.SkuService;
import io.searchbox.client.JestClient;
import io.searchbox.core.Index;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GmallSearchServiceApplicationTests {

	@Reference
	private SkuService skuService;

	@Autowired
	JestClient jestClient;

	@Test
	public void contextLoads() throws IOException {

		//SearchSourceBuilder
		SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

		//bool
		BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();

			//filter
		TermQueryBuilder term1 = new TermQueryBuilder("skuAttrValueList.valueId", 43);
		boolQueryBuilder.filter(term1);


			//must
		MatchQueryBuilder match1 = new MatchQueryBuilder("skuName", "华为");
		boolQueryBuilder.must(match1);

		//query
		searchSourceBuilder.query(boolQueryBuilder);

		//from
		searchSourceBuilder.from(0);

		//size
		searchSourceBuilder.size(20);

		//highlight
		searchSourceBuilder.highlight(null);

		String dslStr = searchSourceBuilder.toString();

		System.out.println(dslStr);

		List<PmsSearchSkuInfo> pmsSearchSkuInfoList = new ArrayList<>();

		Search search = new Search.Builder(dslStr).addIndex("gmall2019pms").addType("PmsSkuInfo").build();
		SearchResult searchResult = jestClient.execute(search);
		List<SearchResult.Hit<PmsSearchSkuInfo, Void>> searchResultHits = searchResult.getHits(PmsSearchSkuInfo.class);
		for (SearchResult.Hit<PmsSearchSkuInfo, Void> searchResultHit : searchResultHits) {
			PmsSearchSkuInfo pmsSearchSkuInfo = searchResultHit.source;
			pmsSearchSkuInfoList.add(pmsSearchSkuInfo);
		}
		System.out.println(pmsSearchSkuInfoList.size());

	}

	@Test
	public void put() throws IOException {

		//查询MySQL数据库
		List<PmsSkuInfo> pmsSkuInfoList = skuService.getAllSku();

		//转为es数据结构
		List<PmsSearchSkuInfo> pmsSearchSkuInfoList = new ArrayList<>();
		for (PmsSkuInfo pmsSkuInfo : pmsSkuInfoList) {
			PmsSearchSkuInfo pmsSearchSkuInfo =new PmsSearchSkuInfo();
			BeanUtils.copyProperties(pmsSkuInfo, pmsSearchSkuInfo);
			pmsSearchSkuInfoList.add(pmsSearchSkuInfo);
		}

		//存入es
		for (PmsSearchSkuInfo pmsSearchSkuInfo : pmsSearchSkuInfoList) {
			Index put = new Index.Builder(pmsSearchSkuInfo)
					.index("gmall2019pms").type("PmsSkuInfo").id(pmsSearchSkuInfo.getId())
					.build();
			jestClient.execute(put);
		}
	}

}
