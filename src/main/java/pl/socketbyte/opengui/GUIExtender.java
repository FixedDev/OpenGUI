package pl.socketbyte.opengui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import pl.socketbyte.opengui.event.ElementResponse;
import pl.socketbyte.opengui.event.WindowResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class GUIExtender implements Listener, WindowResponse {

    private final int id;

    private final List<FinalItemJob> jobs = new ArrayList<>();
    private final Map<Integer, GUIElement> elements = new HashMap<>();
    private GUISettings guiSettings;
    private GUI gui;
    private WindowResponse windowResponse;

    public GUIExtender(GUI gui) {
        this.gui = gui;
        this.id = OpenGUI.INSTANCE.getGUIs().size();
        OpenGUI.INSTANCE.getGUIs().put(id, this);

        this.guiSettings = new GUISettings();
        this.guiSettings.setCanEnterItems(false);
        this.guiSettings.setCanDrag(false);

        Bukkit.getPluginManager().registerEvents(this, OpenGUI.INSTANCE.getInstance());
    }

    public void setGUI(GUI gui) {
        this.gui = gui;
        this.elements.clear();
        this.jobs.clear();
    }

    public void addEmptyElementResponse(int slot, boolean pullable) {
        GUIElement guiElement = new GUIElement(slot, pullable);
        elements.put(slot, guiElement);
    }

    public void addElementResponse(int slot, ElementResponse elementResponse) {
        GUIElement guiElement = new GUIElement(slot);
        guiElement.addElementResponse(slot, elementResponse);
        elements.put(slot, guiElement);
    }

    public void addElementResponse(int slot, boolean pullable, ElementResponse elementResponse) {
        GUIElement guiElement = new GUIElement(slot);
        guiElement.addElementResponse(slot, pullable, elementResponse);
        elements.put(slot, guiElement);
    }

    public void addElementResponse(int slot, GUIExtenderItem guiExtenderItem) {
        GUIElement guiElement = new GUIElement(slot);
        guiElement.addElementResponse(slot, guiExtenderItem.isPullable(), guiExtenderItem);
        elements.put(slot, guiElement);
    }

    private void addEmptyElementResponse(int slot) {
        GUIElement guiElement = new GUIElement(slot);
        elements.put(slot, guiElement);
    }

    public void addWindowResponse(WindowResponse windowResponse) {
        this.windowResponse = windowResponse;
    }


    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().equals(getBukkitInventory())
                && !guiSettings.isCanDrag()) {
            event.setCancelled(true);
            return;
        }
        if (guiSettings.isCanDrag() &&
                canEnter(event.getCursor())) {
            event.setCancelled(false);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView() == null) return;
        if (event.getView().getTopInventory() == null) return;
        if (event.getView().getBottomInventory() == null) return;
        if (event.getClickedInventory() == null) {
            return;
        }

        if(!event.getView().getTopInventory().equals(this.getBukkitInventory())) {
            return;
        }

        if (this.guiSettings.isCanEnterItems() ) {
            if (!event.isShiftClick() && event.getClickedInventory().equals(this.getBukkitInventory()) && event.getCursor() != null) {
                if (this.canEnter(event.getCursor())) {
                    if (this.guiSettings.getEnteredItemResponse() != null) {
                        this.guiSettings.getEnteredItemResponse().event(event);
                    }
                    event.setCancelled(false);
                } else {
                    if (this.guiSettings.getNotEnterableItemResponse() != null) {
                        this.guiSettings.getNotEnterableItemResponse().event(event);
                    }
                    event.setCancelled(true);
                }

                return;
            } else {
                if (!event.getClickedInventory().equals(this.getBukkitInventory()) && event.getCurrentItem() != null) {
                    if (this.canEnter(event.getCurrentItem())) {
                        if (this.guiSettings.getEnteredItemResponse() != null) {
                            this.guiSettings.getEnteredItemResponse().event(event);
                        }
                        event.setCancelled(false);
                    } else {
                        if (this.guiSettings.getNotEnterableItemResponse() != null) {
                            this.guiSettings.getNotEnterableItemResponse().event(event);
                        }
                        event.setCancelled(true);
                    }
                    return;
                }
            }
        } else {
            if (event.getClickedInventory().equals(this.getBukkitInventory())) {
                if (event.isShiftClick()) {
                    event.setCancelled(true);
                } else {
                    if (event.getCurrentItem() == null || event.getCurrentItem().getType().equals(Material.AIR)) {
                        event.setCancelled(true);
                        return;
                    }

                    this.checkElements(event);
                }
                return;
            } else if (event.isShiftClick()) {
                event.setCancelled(true);
                return;
            }

            event.setCancelled(false);
            return;
        }

        this.checkElements(event);
    }

    private boolean canEnter(ItemStack itemStack) {
        if (guiSettings.isCanEnterItems()) {
            List<ItemStack> materials = guiSettings.getEnterableItems();

            if (materials.isEmpty())
                return true;

            if (itemStack == null || itemStack.getType().equals(Material.AIR))
                return true;

            for (ItemStack entry : materials) {
                Material material = entry.getType();
                short data = entry.getDurability();

                if (itemStack.getType().equals(material)
                        && itemStack.getDurability() == data)
                    return true;
            }
        }
        return false;
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent e) {
        if (e.getView().getTopInventory().equals(getBukkitInventory())) {
            if (windowResponse != null)
                windowResponse.onOpen(e);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        if (e.getView().getTopInventory().equals(getBukkitInventory())) {
            if (windowResponse != null)
                windowResponse.onClose(e);

            Bukkit.getScheduler().runTaskLater(OpenGUI.INSTANCE.getInstance(), () ->
                    ((Player) e.getPlayer()).updateInventory(), 5);
            // I need to update the inventory because minecraft is weird
            // otherwise you can make the item to.. "stay" in your inventory until you do something with it
        }
    }

    public void setItem(ItemPack... itemPacks) {
        for (ItemPack itemPack : itemPacks) {
            ItemBuilder itemBuilder = itemPack.getItemBuilder();
            int slot = itemPack.getSlot();

            if (itemPack.getElementResponse() != null)
                addElementResponse(slot, itemPack.getElementResponse());
            else addEmptyElementResponse(slot);

            gui.setItem(slot, itemBuilder);
            updateInventory();
        }
    }

    public void changeItem(int slot, ItemBuilder itemBuilder) {
        gui.setItem(slot, itemBuilder);
        updateInventory();
    }

    public void setItem(int slot, ItemBuilder itemBuilder) {
        gui.setItem(slot, itemBuilder);
        addEmptyElementResponse(slot);
        updateInventory();
    }

    public void setItem(int slot, ItemBuilder itemBuilder, ElementResponse elementResponse) {
        gui.setItem(slot, itemBuilder);
        addElementResponse(slot, elementResponse);
        updateInventory();
    }

    public void setItem(int slot, GUIExtenderItem guiExtenderItem) {
        jobs.add(new FinalItemJob(slot, guiExtenderItem));
    }

    public int addItem(ItemBuilder itemBuilder) {
        int index = gui.addItem(itemBuilder);
        addEmptyElementResponse(index);
        updateInventory();
        return index;
    }

    public int addItem(ItemBuilder itemBuilder, ElementResponse elementResponse) {
        int index = gui.addItem(itemBuilder);
        addElementResponse(index, elementResponse);
        updateInventory();
        return index;
    }

    public void addItem(GUIExtenderItem guiExtenderItem) {
        jobs.add(new FinalItemJob(guiExtenderItem));
    }

    public void removeItem(int slot) {
        gui.removeItem(slot);
    }

    public void openInventory(Player player) {
        if (!jobs.isEmpty()) {
            for (FinalItemJob finalItemJob : jobs) {
                if (finalItemJob.getSlot() == -1) {
                    addExtenderItem(finalItemJob.getGuiExtenderItem(),
                            player);
                } else setExtenderItem(finalItemJob.getSlot(),
                        finalItemJob.getGuiExtenderItem(),
                        player);
            }
            jobs.clear();
        }

        player.openInventory(getBukkitInventory());
    }

    public Inventory getBukkitInventory() {
        return gui.getInventory();
    }

    public void updateInventory() {
        List<Integer> slots = new ArrayList<>();
        int temp = 0;
        for (ItemStack itemStack : getBukkitInventory().getContents()) {
            temp++;
            if (itemStack == null || itemStack.getType().equals(Material.AIR))
                continue;

            int current = temp - 1;
            slots.add(current);
        }

        for (int slot : elements.keySet())
            if (slots.contains(slot))
                slots.remove((Integer) slot);

        for (int slot : slots)
            addEmptyElementResponse(slot);

        getBukkitInventory().getViewers().forEach(viewer -> ((Player) viewer).updateInventory());
    }

    private void setExtenderItem(int slot, GUIExtenderItem guiExtenderItem, Player player) {
        gui.setItem(slot, guiExtenderItem.getItemBuilder(player));
        addElementResponse(slot, guiExtenderItem);
        updateInventory();
    }

    private void addExtenderItem(GUIExtenderItem guiExtenderItem, Player player) {
        int index = gui.addItem(guiExtenderItem.getItemBuilder(player));
        addElementResponse(index, guiExtenderItem);
        updateInventory();
    }

    private void checkElements(InventoryClickEvent event) {
        int eventSlot = event.getSlot();

        if (!event.getClickedInventory().equals(getBukkitInventory()))
            return;
        if (!event.getView().getTopInventory().equals(getBukkitInventory()))
            return;

        GUIElement element = elements.get(eventSlot);

        if (element == null) {
            return;
        }

        event.setCancelled(!element.isPullable());
        if (element.getElementResponse() != null)
            element.getElementResponse().onClick(event);
        else if (element.getGuiExtenderItem() != null)
            element.getGuiExtenderItem().onClick(event);
    }

    public int getId() {
        return id;
    }

    public List<FinalItemJob> getJobs() {
        return jobs;
    }

    public Map<Integer, GUIElement> getElements() {
        return elements;
    }

    public GUISettings getGuiSettings() {
        return guiSettings;
    }

    public GUI getGui() {
        return gui;
    }

    public WindowResponse getWindowResponse() {
        return windowResponse;
    }
}
