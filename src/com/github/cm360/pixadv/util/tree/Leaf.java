package com.github.cm360.pixadv.util.tree;

public class Leaf<T> {

	private T data;
	
	public Leaf(T data) {
		setData(data);
	}
	
	public T getData() {
		return data;
	}
	
	public void setData(T data) {
		this.data = data;
	}

}
