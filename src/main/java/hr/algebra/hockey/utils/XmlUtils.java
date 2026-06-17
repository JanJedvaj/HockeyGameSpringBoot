package hr.algebra.hockey.utils;

import hr.algebra.hockey.model.HockeyMove;
import hr.algebra.hockey.model.HockeyMoveTag;
import hr.algebra.hockey.model.HockeyMoveType;
import hr.algebra.hockey.model.PlayerType;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class XmlUtils {
    private static final String XML_FILE_PATH = "xml/gameMoves.xml";
    private static final String DTD_FILE_PATH = "dtd/gameMoves.dtd";
    private static final String XML_RELATIVE_DTD_FILE_PATH = "xml/dtd/gameMoves.dtd";

    private XmlUtils() {
    }

    public static void resetMoveHistory() throws ParserConfigurationException, TransformerException, IOException {
        Files.createDirectories(Path.of("xml"));
        Files.createDirectories(Path.of("dtd"));
        Document document = createDocument();
        saveDocument(document);
    }

    public static void saveMove(HockeyMove hockeyMove) throws ParserConfigurationException, TransformerException, IOException, SAXException {
        Files.createDirectories(Path.of("xml"));
        Files.createDirectories(Path.of("dtd"));
        Document document = Files.exists(Path.of(XML_FILE_PATH)) ? loadDocument(XML_FILE_PATH, false) : createDocument();
        appendMoveElement(document, hockeyMove);
        saveDocument(document);
    }

    public static List<HockeyMove> loadMoves() {
        if (!Files.exists(Path.of(XML_FILE_PATH))) {
            return new ArrayList<>();
        }

        try {
            ensureXmlRelativeDtd();
            Document document = loadDocument(XML_FILE_PATH, true);
            return readMoves(document);
        } catch (ParserConfigurationException | IOException | SAXException exception) {
            throw new IllegalStateException("Unable to read XML move history.", exception);
        }
    }

    private static void ensureXmlRelativeDtd() throws IOException {
        Path source = Path.of(DTD_FILE_PATH);
        Path target = Path.of(XML_RELATIVE_DTD_FILE_PATH);
        if (Files.exists(source)) {
            Files.createDirectories(target.getParent());
            Files.copy(source, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static Document createDocument() throws ParserConfigurationException {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        DOMImplementation dom = builder.getDOMImplementation();
        DocumentType docType = dom.createDocumentType(HockeyMoveTag.GAME_MOVES.getTagName(), null, DTD_FILE_PATH);
        return dom.createDocument(null, HockeyMoveTag.GAME_MOVES.getTagName(), docType);
    }

    private static Document loadDocument(String path, boolean validating)
            throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(validating);
        DocumentBuilder builder = factory.newDocumentBuilder();
        builder.setErrorHandler(new ErrorHandler() {
            @Override
            public void warning(SAXParseException exception) throws SAXException {
                throw exception;
            }

            @Override
            public void error(SAXParseException exception) throws SAXException {
                throw exception;
            }

            @Override
            public void fatalError(SAXParseException exception) throws SAXException {
                throw exception;
            }
        });
        return builder.parse(new File(path));
    }

    private static void saveDocument(Document document) throws TransformerException, IOException {
        
        ensureXmlRelativeDtd();
        Transformer transformer = TransformerFactory.newDefaultInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, DTD_FILE_PATH);
        transformer.transform(new DOMSource(document), new StreamResult(new File(XML_FILE_PATH)));
    }

    private static void appendMoveElement(Document document, HockeyMove hockeyMove) {
        Element moveElement = document.createElement(HockeyMoveTag.HOCKEY_MOVE.getTagName());
        document.getDocumentElement().appendChild(moveElement);

        moveElement.appendChild(createElement(document, HockeyMoveTag.MOVE_TYPE, hockeyMove.getMoveType().name()));
        moveElement.appendChild(createElement(document, HockeyMoveTag.PLAYER_TYPE, hockeyMove.getPlayerType().name()));
        moveElement.appendChild(createElement(document, HockeyMoveTag.TIMESTAMP, hockeyMove.getTimestamp().toString()));
        moveElement.appendChild(createElement(document, HockeyMoveTag.PLAYER_ONE_SCORE, String.valueOf(hockeyMove.getPlayerOneScore())));
        moveElement.appendChild(createElement(document, HockeyMoveTag.PLAYER_TWO_SCORE, String.valueOf(hockeyMove.getPlayerTwoScore())));
        moveElement.appendChild(createElement(document, HockeyMoveTag.PLAYER_ONE_X, String.valueOf(hockeyMove.getPlayerOneX())));
        moveElement.appendChild(createElement(document, HockeyMoveTag.PLAYER_ONE_Y, String.valueOf(hockeyMove.getPlayerOneY())));
        moveElement.appendChild(createElement(document, HockeyMoveTag.PLAYER_TWO_X, String.valueOf(hockeyMove.getPlayerTwoX())));
        moveElement.appendChild(createElement(document, HockeyMoveTag.PLAYER_TWO_Y, String.valueOf(hockeyMove.getPlayerTwoY())));
        moveElement.appendChild(createElement(document, HockeyMoveTag.PUCK_X, String.valueOf(hockeyMove.getPuckX())));
        moveElement.appendChild(createElement(document, HockeyMoveTag.PUCK_Y, String.valueOf(hockeyMove.getPuckY())));
        moveElement.appendChild(createElement(document, HockeyMoveTag.TIME_LEFT, String.valueOf(hockeyMove.getTimeLeft())));
    }

    private static Node createElement(Document document, HockeyMoveTag tag, String data) {
        Element element = document.createElement(tag.getTagName());
        Text text = document.createTextNode(data);
        element.appendChild(text);
        return element;
    }

    private static List<HockeyMove> readMoves(Document document) {
        List<HockeyMove> hockeyMoves = new ArrayList<>();
        NodeList nodes = document.getElementsByTagName(HockeyMoveTag.HOCKEY_MOVE.getTagName());
        for (int i = 0; i < nodes.getLength(); i++) {
            Element element = (Element) nodes.item(i);
            HockeyMove move = new HockeyMove();
            move.setMoveType(HockeyMoveType.valueOf(text(element, HockeyMoveTag.MOVE_TYPE)));
            move.setPlayerType(PlayerType.valueOf(text(element, HockeyMoveTag.PLAYER_TYPE)));
            move.setTimestamp(LocalDateTime.parse(text(element, HockeyMoveTag.TIMESTAMP)));
            move.setPlayerOneScore(Integer.parseInt(text(element, HockeyMoveTag.PLAYER_ONE_SCORE)));
            move.setPlayerTwoScore(Integer.parseInt(text(element, HockeyMoveTag.PLAYER_TWO_SCORE)));
            move.setPlayerOneX(Double.parseDouble(text(element, HockeyMoveTag.PLAYER_ONE_X)));
            move.setPlayerOneY(Double.parseDouble(text(element, HockeyMoveTag.PLAYER_ONE_Y)));
            move.setPlayerTwoX(Double.parseDouble(text(element, HockeyMoveTag.PLAYER_TWO_X)));
            move.setPlayerTwoY(Double.parseDouble(text(element, HockeyMoveTag.PLAYER_TWO_Y)));
            move.setPuckX(Double.parseDouble(text(element, HockeyMoveTag.PUCK_X)));
            move.setPuckY(Double.parseDouble(text(element, HockeyMoveTag.PUCK_Y)));
            move.setTimeLeft(Integer.parseInt(text(element, HockeyMoveTag.TIME_LEFT)));
            hockeyMoves.add(move);
        }
        return hockeyMoves;
    }

    private static String text(Element element, HockeyMoveTag tag) {
        return element.getElementsByTagName(tag.getTagName()).item(0).getTextContent();
    }
}