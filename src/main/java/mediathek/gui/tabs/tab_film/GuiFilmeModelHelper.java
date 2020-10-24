package mediathek.gui.tabs.tab_film;

import javafx.collections.ObservableList;
import mediathek.controller.history.SeenHistoryController;
import mediathek.daten.DatenFilm;
import mediathek.daten.ListeFilme;
import mediathek.javafx.filterpanel.FilmActionPanel;
import mediathek.javafx.filterpanel.FilmLengthSlider;
import mediathek.tool.Filter;
import mediathek.tool.models.TModelFilm;
import org.jetbrains.annotations.NotNull;

import javax.swing.table.TableModel;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class GuiFilmeModelHelper {
    private final FilmActionPanel fap;
    private final TModelFilm filmModel;
    private final ListeFilme listeFilme;
    private final SeenHistoryController historyController;
    private boolean searchThroughDescriptions;
    private boolean nurNeue;
    private boolean onlyBookMarked;
    private boolean nurUt;
    private boolean showOnlyHd;
    private boolean kGesehen;
    private boolean keineAbos;
    private boolean showOnlyLivestreams;
    private boolean dontShowTrailers;
    private boolean dontShowGebaerdensprache;
    private boolean dontShowAudioVersions;
    private long maxLength;
    private String[] arrIrgendwo;
    private long minLengthInSeconds;
    private long maxLengthInSeconds;

    public GuiFilmeModelHelper(@NotNull FilmActionPanel fap, @NotNull ListeFilme filteredList,
                               @NotNull SeenHistoryController historyController) {
        this.fap = fap;
        this.historyController = historyController;

        filmModel = new TModelFilm();
        listeFilme = filteredList;

    }

    private String getFilterThema() {
        String filterThema = fap.themaBox.getSelectionModel().getSelectedItem();
        if (filterThema == null) {
            filterThema = "";
        }

        return filterThema;
    }

    private String[] evaluateThemaTitel() {
        String[] arrThemaTitel;

        final String filterThemaTitel = fap.roSearchStringProperty.getValueSafe();
        if (Filter.isPattern(filterThemaTitel)) {
            arrThemaTitel = new String[]{filterThemaTitel};
        } else {
            arrThemaTitel = filterThemaTitel.split(",");
            for (int i = 0; i < arrThemaTitel.length; ++i) {
                arrThemaTitel[i] = arrThemaTitel[i].trim().toLowerCase();
            }
        }

        return arrThemaTitel;
    }

    private boolean noFiltersAreSet() {
        boolean ret = false;

        if (fap.senderList.getCheckModel().isEmpty()
                && getFilterThema().isEmpty()
                && fap.roSearchStringProperty.getValueSafe().isEmpty()
                && ((int) fap.filmLengthSlider.getLowValue() == 0)
                && ((int) fap.filmLengthSlider.getHighValue() == FilmLengthSlider.UNLIMITED_VALUE)
                && !fap.dontShowAbos.getValue()
                && !fap.showUnseenOnly.getValue()
                && !fap.showOnlyHd.getValue()
                && !fap.showSubtitlesOnly.getValue()
                && !fap.showLivestreamsOnly.getValue()
                && !fap.showNewOnly.getValue()
                && !fap.showBookMarkedOnly.getValue()
                && !fap.dontShowTrailers.getValue()
                && !fap.dontShowSignLanguage.getValue()
                && !fap.dontShowAudioVersions.getValue())
            ret = true;

        return ret;
    }

    private void updateFilterVars() {
        nurNeue = fap.showNewOnly.getValue();
        onlyBookMarked = fap.showBookMarkedOnly.getValue();
        nurUt = fap.showSubtitlesOnly.getValue();
        showOnlyHd = fap.showOnlyHd.getValue();
        kGesehen = fap.showUnseenOnly.getValue();
        keineAbos = fap.dontShowAbos.getValue();
        showOnlyLivestreams = fap.showLivestreamsOnly.getValue();
        dontShowTrailers = fap.dontShowTrailers.getValue();
        dontShowGebaerdensprache = fap.dontShowSignLanguage.getValue();
        dontShowAudioVersions = fap.dontShowAudioVersions.getValue();
        searchThroughDescriptions = fap.searchThroughDescription.getValue();

        arrIrgendwo = evaluateThemaTitel();
    }

    private void calculateFilmLengthSliderValues() {
        final long minLength = (long) fap.filmLengthSlider.getLowValue();
        maxLength = (long) fap.filmLengthSlider.getHighValue();
        minLengthInSeconds = TimeUnit.SECONDS.convert(minLength, TimeUnit.MINUTES);
        maxLengthInSeconds = TimeUnit.SECONDS.convert(maxLength, TimeUnit.MINUTES);
    }

    private void performTableFiltering() {
        updateFilterVars();
        calculateFilmLengthSliderValues();

        final String filterThema = getFilterThema();
        final boolean searchFieldEmpty = arrIrgendwo.length == 0;
        final ObservableList<String> selectedSenders = fap.senderList.getCheckModel().getCheckedItems();

        historyController.prepareMemoryCache();

        var stream = listeFilme.parallelStream();
        if (nurNeue)
            stream = stream.filter(DatenFilm::isNew);
        if (onlyBookMarked)
            stream = stream.filter(DatenFilm::isBookmarked);
        if (showOnlyLivestreams)
            stream = stream.filter(DatenFilm::isLivestream);
        if (showOnlyHd)
            stream = stream.filter(DatenFilm::isHighQuality);
        if (dontShowTrailers)
            stream = stream.filter(film -> !film.isTrailerTeaser());
        if (dontShowGebaerdensprache)
            stream = stream.filter(film -> !film.isSignLanguage());
        if (dontShowAudioVersions)
            stream = stream.filter(film -> !film.isAudioVersion());
        if (keineAbos)
            stream = stream.filter(film -> film.getAboName().isEmpty());
        if (nurUt) {
            stream = stream.filter(film -> film.hasSubtitle() || film.hasBurnedInSubtitles());
        }
        if (kGesehen) {
            stream = stream.filter(film -> !historyController.hasBeenSeenFromCache(film));
        }

        var filteredList = stream.collect(Collectors.toList());

        for (DatenFilm film : filteredList) {
            if (!selectedSenders.isEmpty()) {
                if (!selectedSenders.contains(film.getSender()))
                    continue;
            }

            final long filmLength = film.getFilmLength();
            //film entries without length have internal length 0...
            if (filmLength != 0) {
                if (filmLength < minLengthInSeconds)
                    continue;
            }

            if (maxLength < FilmLengthSlider.UNLIMITED_VALUE) {
                if (filmLength > maxLengthInSeconds)
                    continue;

            }

            if (!filterThema.isEmpty()) {
                if (!film.getThema().equalsIgnoreCase(filterThema))
                    continue;
            }

            //minor speedup in case we don´t have search field entries...
            if (searchFieldEmpty)
                addFilmToTableModel(film);
            else {
                if (finalStageFiltering(film)) {
                    addFilmToTableModel(film);
                }
            }
        }

        filteredList.clear();
        historyController.emptyMemoryCache();
    }

    /**
     * Perform the last stage of filtering.
     * Rework!!!
     */
    private boolean finalStageFiltering(final DatenFilm film) {
        boolean result;

        if (searchThroughDescriptions && !film.getDescription().isEmpty())
            result = searchEntriesWithDescription(film);
        else
            result = searchEntries(film);

        return result;
    }

    private boolean searchEntries(DatenFilm film) {
        boolean result = false;
        if (Filter.pruefen(arrIrgendwo, film.getThema())
                || Filter.pruefen(arrIrgendwo, film.getTitle())) {
            result = true;
        }
        return result;
    }

    private boolean searchEntriesWithDescription(DatenFilm film) {
        boolean result = false;

        if (Filter.pruefen(arrIrgendwo, film.getDescription())
                || searchEntries(film)) {
            result = true;
        }

        return result;
    }

    private void fillTableModel() {
        // dann ein neues Model anlegen
        if (noFiltersAreSet()) {
            // dann ganze Liste laden
            addAllFilmsToTableModel();
        } else {
            performTableFiltering();
        }
    }

    /**
     * Filter the filmlist.
     *
     * @return the filtered table model.
     */
    public TableModel getFilteredTableModel() {
        if (!listeFilme.isEmpty())
            fillTableModel();
        return filmModel;
    }

    private void addAllFilmsToTableModel() {
        if (!listeFilme.isEmpty()) {
            for (DatenFilm film : listeFilme) {
                addFilmToTableModel(film);
            }
        }
    }

    private void addFilmToTableModel(DatenFilm film) {
        Object[] object = new Object[1];
        object[0] = film;

        filmModel.addRow(object);
    }
}
