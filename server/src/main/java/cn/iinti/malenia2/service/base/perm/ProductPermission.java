package cn.iinti.malenia2.service.base.perm;

import cn.iinti.malenia2.entity.Product;
import cn.iinti.malenia2.mapper.ProductMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.stream.Collectors;

@Service
public class ProductPermission extends Permission<Product> {

    @Resource
    private ProductMapper productMapper;

    public ProductPermission() {
        super(Product.class);
    }

    @Override
    public String scope() {
        return "product";
    }


    @Override
    public Collection<String> perms() {
        return productMapper.selectList(new QueryWrapper<>()).stream()
                .map(Product::getProductName)
                .collect(Collectors.toList());
    }

    @Override
    public void filter(Collection<String> perms, QueryWrapper<Product> sql) {
        QueryWrapper<Product> eq = sql.eq(Product.PRIVATE_PRODUCT, false);
        if (!perms.isEmpty()) {
            eq.or(child -> child.in(Product.PRODUCT_NAME, perms));
        }
    }

    @Override
    public boolean hasPermission(Collection<String> perms, Product product) {
        return !product.getPrivateProduct() || perms.contains(product.getProductName());
    }
}
