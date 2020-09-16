package cn.hzw.doodledemo;

import java.util.Objects;

class Legend {
	private String symbol;
	private String name;

	public String getSymbol() {
		return symbol;
	}

	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Legend legend = (Legend) o;
		return getSymbol().equals(legend.getSymbol());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getSymbol());
	}

	@Override
	public String toString() {
		return "Legend{" +
				"symbol='" + symbol + '\'' +
				", name='" + name + '\'' +
				'}';
	}
}
