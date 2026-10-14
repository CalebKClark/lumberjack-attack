namespace LumberjackAttack.Items;

public readonly struct ItemStack
{
	public string? ItemId { get; }
	public int Quantity { get; }

	private ItemStack(string? itemId, int quantity)
	{
		ItemId = itemId;
		Quantity = quantity;
	}

	public bool IsEmpty => string.IsNullOrEmpty(ItemId) || Quantity <= 0;

	public static ItemStack Empty => new(null, 0);

	public static ItemStack Of(string itemId, int quantity)
	{
		if (string.IsNullOrWhiteSpace(itemId) || quantity <= 0)
		{
			return Empty;
		}

		return new ItemStack(itemId, quantity);
	}

	public ItemStack WithQuantity(int quantity) =>
		quantity <= 0 || string.IsNullOrEmpty(ItemId) ? Empty : new ItemStack(ItemId, quantity);
}
