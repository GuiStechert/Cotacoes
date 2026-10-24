package br.com.cotacao.application;
/** Adaptador deve fornecer uma conta validada pelo autenticador, nunca um cabeçalho arbitrário. */
@FunctionalInterface
public interface Identidade { String contaAutenticada(); }
