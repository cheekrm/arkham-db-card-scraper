package com.ryancheek;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.DomNode;
import com.gargoylesoftware.htmlunit.html.DomNodeList;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Application {

    private static final Logger LOGGER = Logger.getLogger(Application.class.getCanonicalName());

    private static final List<String> cardSetUrls = Arrays.asList(
            "https://arkhamdb.com/set/core",
            "https://arkhamdb.com/cycle/dwl",
            "https://arkhamdb.com/cycle/ptc",
            "https://arkhamdb.com/cycle/tfa",
            "https://arkhamdb.com/cycle/tcu",
            "https://arkhamdb.com/cycle/tde",
            "https://arkhamdb.com/cycle/tic",
            "https://arkhamdb.com/cycle/return",
            "https://arkhamdb.com/cycle/investigator",
            "https://arkhamdb.com/cycle/side_stories",
            "https://arkhamdb.com/cycle/promotional",
            "https://arkhamdb.com/cycle/parallel"
    );

    public static void main(final String[] args) {

        final WebClient client = new WebClient();
        client.getOptions().setCssEnabled(false);
        client.getOptions().setJavaScriptEnabled(false);

        final String urlPrefix = "https://arkhamdb.com/card/";
        final String arkhamDbUrlPrefix = "https://arkhamdb.com";

        cardSetUrls.stream()
                .peek(cardSetUrl -> System.out.printf("Collecting cards for page %s", cardSetUrl))
                .map(cardSetUrl -> {
                    try {
                        return client.getPage(cardSetUrl);
                    } catch (IOException e) {
                        e.printStackTrace();
                        return new HtmlPage(null, null, null);
                    }
                })
                .map(setPage -> {
                    try {

                        final DomNodeList<DomNode> domNodes = setPage.querySelectorAll("a.spoiler.card-tip");

                        return domNodes.stream().map(node -> node.getAttributes().getNamedItem("data-code").getNodeValue())
                                .map(dataCode -> {

                                    final String fullyQualifiedUrl = urlPrefix + dataCode;

                                    try {
                                        final HtmlPage cardPage = client.getPage(fullyQualifiedUrl);

                                        final String name = Optional.ofNullable(cardPage.querySelectorAll("a.card-name.card-tip").get(0))
                                                .map(DomNode::getTextContent)
                                                .orElse(UUID.randomUUID().toString());

                                        final String imgSrc = Optional.ofNullable(cardPage.querySelectorAll("img.img-responsive.img-vertical-card").get(0))
                                                .map(imageNode -> imageNode.getAttributes().getNamedItem("src").getTextContent())
                                                .orElse(fullyQualifiedUrl);

                                        return new Card(name, imgSrc);
                                    } catch (final Exception e) {
                                        return new Card(UUID.randomUUID().toString(), fullyQualifiedUrl);
                                    }
                                }).collect(Collectors.toMap(
                                        Card::getName,
                                        card -> card,
                                        (k1, k2) -> k1
                                ));
                    } catch (final Exception e) {
                        return new HashMap<String, Card>();
                    }
                })
                .peek(stringCardMap -> System.out.printf("Page finished. %s cards collected.\n", stringCardMap.size()))
                .reduce((firstMap, secondMap) ->
                Stream.concat(firstMap.entrySet().stream(), secondMap.entrySet().stream())
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                                (countInFirstMap, countInSecondMap) -> countInFirstMap))
        ).ifPresent(cardMap -> {
            final ObjectMapper objectMapper = new ObjectMapper();

            try {
                final String cardsJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(cardMap);

                final BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(new File("cards.json")));
                bufferedWriter.write(cardsJson);
                bufferedWriter.close();
            } catch (final IOException e) {
                LOGGER.log(Level.SEVERE, e.getMessage());
            }
        });
    }
}

class Card {
    final String name;
    final String imgSrc;

    public Card(String name, String imgSrc) {
        this.name = name;
        this.imgSrc = imgSrc;
    }

    public String getName() {
        return name;
    }

    public String getImgSrc() {
        return imgSrc;
    }
}