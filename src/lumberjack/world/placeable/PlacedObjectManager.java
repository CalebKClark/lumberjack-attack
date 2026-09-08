package lumberjack.world.placeable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import lumberjack.entity.PlacedObject;
import lumberjack.save.SaveData;
import lumberjack.world.map.TileMap;

/**
 * Tracks placeable objects placed on map tiles.
 */
public final class PlacedObjectManager {

    private final List<PlacedObject> placedObjects = new ArrayList<>();

    public void place(String mapId, String itemId, int col, int row) {
        placedObjects.add(new PlacedObject(mapId, itemId, col, row));
    }

    public boolean hasAt(String mapId, int col, int row) {
        for (PlacedObject placedObject : placedObjects) {
            if (placedObject.getMapId().equals(mapId) && placedObject.occupiesTile(col, row)) {
                return true;
            }
        }
        return false;
    }

    public PlacedObject findAt(String mapId, int col, int row) {
        for (PlacedObject placedObject : placedObjects) {
            if (placedObject.getMapId().equals(mapId) && placedObject.occupiesTile(col, row)) {
                return placedObject;
            }
        }
        return null;
    }

    public PlacedObject removeAt(String mapId, int col, int row) {
        Iterator<PlacedObject> iterator = placedObjects.iterator();
        while (iterator.hasNext()) {
            PlacedObject placedObject = iterator.next();
            if (placedObject.getMapId().equals(mapId) && placedObject.occupiesTile(col, row)) {
                iterator.remove();
                return placedObject;
            }
        }
        return null;
    }

    public void clear() {
        placedObjects.clear();
    }

    public List<SaveData.PlacedObjectEntry> exportPlacedObjects() {
        List<SaveData.PlacedObjectEntry> entries = new ArrayList<>();
        for (PlacedObject placedObject : placedObjects) {
            entries.add(new SaveData.PlacedObjectEntry(
                    placedObject.getMapId(),
                    placedObject.getItemId(),
                    placedObject.getCol(),
                    placedObject.getRow()
            ));
        }
        return entries;
    }

    public void restorePlacedObject(String mapId, String itemId, int col, int row) {
        if (!hasAt(mapId, col, row)) {
            place(mapId, itemId, col, row);
        }
    }

    public void restoreAll(List<SaveData.PlacedObjectEntry> entries, java.util.function.Function<String, TileMap> mapLookup) {
        clear();
        for (SaveData.PlacedObjectEntry entry : entries) {
            TileMap map = mapLookup.apply(entry.getMapId());
            if (map != null) {
                place(entry.getMapId(), entry.getItemId(), entry.getCol(), entry.getRow());
            }
        }
    }

    public PlacedObject findAtWorldPosition(String mapId, int worldX, int worldY) {
        for (PlacedObject placedObject : placedObjects) {
            if (!mapId.equals(placedObject.getMapId())) {
                continue;
            }
            if (placedObject.getBounds().contains(worldX, worldY)) {
                return placedObject;
            }
        }
        return null;
    }

    public boolean collidesWith(java.awt.Rectangle bounds, String mapId) {
        for (PlacedObject placedObject : placedObjects) {
            if (!mapId.equals(placedObject.getMapId()) || !placedObject.blocksMovement()) {
                continue;
            }
            if (bounds.intersects(placedObject.getBounds())) {
                return true;
            }
        }
        return false;
    }

    /** Live placeables on a map (for LibGDX renderer). */
    public List<PlacedObject> getOnMap(String mapId) {
        List<PlacedObject> results = new ArrayList<>();
        for (PlacedObject placedObject : placedObjects) {
            if (mapId.equals(placedObject.getMapId())) {
                results.add(placedObject);
            }
        }
        return results;
    }
}
