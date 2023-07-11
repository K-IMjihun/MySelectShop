package com.sparta.myselectshop.service;

import com.sparta.myselectshop.dto.ProductMypriceRequestDto;
import com.sparta.myselectshop.dto.ProductRequestDto;
import com.sparta.myselectshop.dto.ProductResponseDto;
import com.sparta.myselectshop.entity.*;
import com.sparta.myselectshop.exception.ProductNotFoundException;
import com.sparta.myselectshop.naver.dto.ItemDto;
import com.sparta.myselectshop.repository.FolderRepository;
import com.sparta.myselectshop.repository.ProductFolderRepository;
import com.sparta.myselectshop.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    private final FolderRepository folderRepository;

    private final ProductFolderRepository productFolderRepository;

    private final MessageSource messageSource;
    public static final int MIN_MY_PRICE = 100;
    public ProductResponseDto createProduct(ProductRequestDto requestDto, User user) {
        Product product = productRepository.save(new Product(requestDto, user));
        return new ProductResponseDto(product);
    }

    @Transactional
    public ProductResponseDto updateProduct(Long id, ProductMypriceRequestDto requestDto) {
        int myprice = requestDto.getMyprice();
        if(myprice < MIN_MY_PRICE){
            throw new IllegalArgumentException(messageSource.getMessage(
                    "below.min.my.price",
                    new Integer[]{MIN_MY_PRICE},
                    "Wrong Price",
                    Locale.getDefault() // 언어설정
            ));
        }



        Product product = productRepository.findById(id).orElseThrow(() ->
                new ProductNotFoundException(messageSource.getMessage(
                        "not.found.product",
                        null,
                        "Not Found Product",
                        Locale.getDefault()
                )));
        product.update(requestDto);

        return new ProductResponseDto(product);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponseDto> getProducts(User user, int page, int size, String sortBy, boolean isAsc) {

        Pageable pageable = paging(page, size, sortBy, isAsc);

        // 권한 확인
        UserRoleEnum userRoleEnum = user.getRole();
        Page<Product> productList;
        if(userRoleEnum == UserRoleEnum.USER){
            productList = productRepository.findAllByUser(user, pageable);
        } else {
            productList = productRepository.findAll(pageable);
        }

        return productList.map(ProductResponseDto::new);
    }


    public void addFolder(Long productId, Long folderId, User user) {
        Product product = productRepository.findById(productId).orElseThrow(() ->
                new NullPointerException("해당 상품이 존재하지 않습니다.")
        );

        Folder folder = folderRepository.findById(folderId).orElseThrow(() ->
                new NullPointerException("해당 폴더가 존재하지 않습니다")
        );

        if(!product.getUser().getId().equals(user.getId())
        || !folder.getUser().getId().equals(user.getId())){
            throw new IllegalArgumentException("회원님의 관심 상품이 아니거나 회원님의 폴더가 아닙니다.");
        }
        // 폴더 중복확인
       Optional<ProductFolder> overlapFolder = productFolderRepository.findByProductAndFolder(product, folder);

        if (overlapFolder.isPresent()){
            throw new IllegalArgumentException("중복된 폴더입니다.");
        }

        productFolderRepository.save(new ProductFolder(product, folder));

    }

    @Transactional
    public void updateBySearch(Long id, ItemDto itemDto) {
        Product product = productRepository.findById(id).orElseThrow(()->
                new NullPointerException("해당 상품은 존재하지 않습니다.")
        );
        product.updateByItemDto(itemDto);
    }

    public Page<ProductResponseDto> getProductsInFolder(Long folderId, int page, int size, String sortBy, boolean isAsc, User user) {
        Pageable pageable = paging(page, size, sortBy, isAsc);

        Page<Product> productList = productRepository.findAllByUserAndProductFolderList_FolderId(user, folderId, pageable);

        Page<ProductResponseDto> responseDtoList = productList.map(ProductResponseDto::new);
        return responseDtoList;
    }

    private Pageable paging(int page, int size, String sortBy, boolean isAsc) {
        // 정렬
        Sort.Direction direction = isAsc ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = Sort.by(direction, sortBy);

        // pageable 생성
        return PageRequest.of(page, size, sort);
    }
}
