package com.ihorshulha.asyncapidatamanager.service;

import com.ihorshulha.asyncapidatamanager.dto.CompanyDTO;
import com.ihorshulha.asyncapidatamanager.entity.Company;
import com.ihorshulha.asyncapidatamanager.entity.Stock;
import com.ihorshulha.asyncapidatamanager.mapper.CompanyMapper;
import com.ihorshulha.asyncapidatamanager.mapper.StockMapper;
import com.ihorshulha.asyncapidatamanager.repository.CompanyRepository;
import com.ihorshulha.asyncapidatamanager.repository.StockRepository;
import com.ihorshulha.asyncapidatamanager.util.ExApiExchangeClient;
import com.ihorshulha.asyncapidatamanager.util.QueueClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


@Slf4j
@Service
@RequiredArgsConstructor
public class DataProcessingService {

    @Value("${service.numberOfCompanies}")
    private Integer NUMBER_OF_COMPANIES;

    private final ExApiExchangeClient apiClient;
    private final QueueClient queueClient;
    private final CompanyRepository companyRepository;
    private final StockRepository stockRepository;
    private final CompanyMapper companyMapper;
    private final StockMapper stockMapper;

    protected List<Company> processingOfCompanyData() {
        return apiClient.getCompanies().stream()
                .filter(CompanyDTO::isEnabled)
                .limit(NUMBER_OF_COMPANIES)
                .map(companyDTO -> {
                    queueClient.putToQueue(apiClient.getStockPriceUrl(companyDTO.symbol()));
                    return companyMapper.companyDtoToCompany(companyDTO);
                })
                .toList();
    }

    public List<Stock> processingOfStocksData() {
        ExecutorService executor = Executors.newCachedThreadPool();

        return queueClient.getCompanyQueue().stream()
                .map(task -> CompletableFuture.supplyAsync(() -> apiClient.getOneCompanyStock(queueClient.takeUrl()), executor))
                .map(contentFuture -> contentFuture.thenApply(Optional::orElseThrow))
                .map(stockDtoFuture -> stockDtoFuture.thenApply(stockMapper::stockDtoToStock))
                .map(CompletableFuture::join)
                .toList();
    }

    protected void saveCompanies(List<Company> companies) {
        Flux.from(companyRepository.saveAll(companies)).subscribe();
        log.debug("storing companies was completed");
    }

    protected void saveStocks(List<Stock> stocks) {
        Flux.from(stockRepository.saveAll(stocks)).subscribe();
        log.debug("storing stocks was completed");
    }
}
