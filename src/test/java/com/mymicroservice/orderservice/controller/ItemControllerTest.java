package com.mymicroservice.orderservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mymicroservice.orderservice.configuration.SecurityConfig;
import com.mymicroservice.orderservice.dto.ItemDto;
import com.mymicroservice.orderservice.exception.ItemNotFoundException;
import com.mymicroservice.orderservice.mapper.ItemMapper;
import com.mymicroservice.orderservice.model.Item;
import com.mymicroservice.orderservice.service.ItemService;
import com.mymicroservice.orderservice.util.ItemGenerator;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc(addFilters = false)
@Import(SecurityConfig.class)
@WithMockUser(roles = {"ADMIN", "USER"})
@WebMvcTest(ItemController.class)
@Slf4j
public class ItemControllerTest {

    @MockBean
    private ItemService itemService;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private final static Long ITEM_ID = 1L;
    private Item testItem;
    private ItemDto testItemDto;


    @BeforeEach
    void setUp() {
        testItem = ItemGenerator.generateItem();
        testItem.setId(ITEM_ID);

        testItemDto = ItemMapper.INSTANSE.toDto(testItem);
    }

    @Test
    public void getItemById_ShouldReturnItemDto() throws Exception {
        log.info("▶ Running test: getItemById_ShouldReturnItemDto(), ITEM_ID={}", ITEM_ID);
        when(itemService.getItemById(ITEM_ID)).thenReturn(testItemDto);

        mockMvc.perform(get("/order-service/api/items/{id}", ITEM_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ITEM_ID));

        verify(itemService).getItemById(ITEM_ID);
    }

    @Test
    public void getItemById_ShouldReturnNotFound() throws Exception {
        log.info("▶ Running test: getItemById_ShouldReturnNotFound(), _ITEM_ID={}", ITEM_ID);
        when(itemService.getItemById(ITEM_ID)).thenReturn(null);

        mockMvc.perform(get("/order-service/api/items/{id}", ITEM_ID))
                .andExpect(status().isNotFound());

        verify(itemService).getItemById(ITEM_ID);
    }

    @Test
    public void createItem_ShouldReturnCreatedItemDto() throws Exception {
        log.info("▶ Running test: createItem_ShouldReturnCreatedItemDto, Item={}", testItemDto);
        when(itemService.createItem(any(ItemDto.class))).thenReturn(testItemDto);

        mockMvc.perform(post("/order-service/api/items/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testItemDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ITEM_ID));

        verify(itemService).createItem(any(ItemDto.class));
    }

    @Test
    public void updateItem_ShouldReturnUpdatedItemDto() throws Exception {
        ItemDto updatedDto = ItemMapper.INSTANSE.toDto(ItemGenerator.generateItem());
        updatedDto.setId(1L);
        updatedDto.setName("UpdatedName");
        updatedDto.setPrice(BigDecimal.valueOf(101));
        log.info("▶ Running test: updateItem_ShouldReturnUpdatedItemDto, UPDATED_ITEM={}", updatedDto);

        when(itemService.updateItem(ITEM_ID, updatedDto)).thenReturn(updatedDto);

        mockMvc.perform(put("/order-service/api/items/{id}", ITEM_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ITEM_ID))
                .andExpect(jsonPath("$.price").value(updatedDto.getPrice()));

        verify(itemService).updateItem(ITEM_ID, updatedDto);
    }

    @Test
    public void updateItem_ShouldReturnNotFound() throws Exception {
        ItemDto updatedDto = ItemMapper.INSTANSE.toDto(ItemGenerator.generateItem());
        updatedDto.setId(1L);
        updatedDto.setName("UpdatedName");
        updatedDto.setPrice(BigDecimal.valueOf(101));
        log.info("▶ Running test: updateItem_ShouldReturnNotFound, UPDATED_ITEM={}", updatedDto);

        when(itemService.updateItem(ITEM_ID, updatedDto))
                .thenThrow(new ItemNotFoundException("Item wasn't found with id " + ITEM_ID));

        mockMvc.perform(put("/order-service/api/items/{id}", ITEM_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedDto)))
                .andExpect(status().isNotFound());

        verify(itemService).updateItem(ITEM_ID, updatedDto);
    }

    @Test
    public void deleteItem_ShouldReturnDeletedItemDto() throws Exception {
        log.info("▶ Running test: deleteItem_ShouldReturnDeletedItemDto, _ITEM_ID={}", ITEM_ID);
        when(itemService.deleteItem(ITEM_ID)).thenReturn(testItemDto);

        mockMvc.perform(delete("/order-service/api/items/{id}", ITEM_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ITEM_ID))
                .andExpect(jsonPath("$.price").value(testItemDto.getPrice()));

        verify(itemService).deleteItem(ITEM_ID);
    }

    @Test
    public void deleteItem_ShouldReturnNotFound() throws Exception {
        log.info("▶ Running test: deleteItem_ShouldReturnNotFound, ITEM_ID={}", ITEM_ID);
        when(itemService.deleteItem(ITEM_ID)).thenReturn(null);

        mockMvc.perform(delete("/order-service/api/items/{id}", ITEM_ID))
                .andExpect(status().isNotFound());

        verify(itemService).deleteItem(ITEM_ID);
    }

    @Test
    public void getItemsIdIn_ShouldReturnItemsForGivenIds() throws Exception {
        Set<Long> ids = Set.of(ITEM_ID);
        log.info("▶ Running test: getItemsIdIn_ShouldReturnItemsForGivenIds, ids={}", ids);
        when(itemService.getItemsIdIn(ids)).thenReturn(List.of(testItemDto));

        mockMvc.perform(get("/order-service/api/items/find-by-ids")
                        .param("ids", ITEM_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(ITEM_ID));

        verify(itemService).getItemsIdIn(ids);
    }

    @Test
    public void getItemsIdIn_ShouldReturnEmptyListWhenNoMatches() throws Exception {
        Set<Long> ids = Set.of(999L);
        log.info("▶ Running test: getItemsIdIn_ShouldReturnEmptyListWhenNoMatches, ids={}", ids);
        when(itemService.getItemsIdIn(ids)).thenReturn(List.of());

        mockMvc.perform(get("/order-service/api/items/find-by-ids")
                        .param("ids", "999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());

        verify(itemService).getItemsIdIn(ids);
    }

    @Test
    public void getAllItems_ShouldReturnAllItems() throws Exception {
        log.info("▶ Running test: getAllItems_ShouldReturnAllItems");
        when(itemService.getAllItems()).thenReturn(List.of(testItemDto));

        mockMvc.perform(get("/order-service/api/items/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(ITEM_ID));

        verify(itemService).getAllItems();
    }

    @Test
    public void getAllItemsWithPagination_ShouldReturnPaginatedItems() throws Exception {
        log.info("▶ Running test: getAllItemsWithPagination_ShouldReturnPaginatedItems, page=0, size=10");
        Page<ItemDto> page = new PageImpl<>(List.of(testItemDto));
        when(itemService.getAllItemsNativeWithPagination(0, 10)).thenReturn(page);

        mockMvc.perform(get("/order-service/api/items/paginated")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(ITEM_ID));

        verify(itemService).getAllItemsNativeWithPagination(0, 10);
    }
}
