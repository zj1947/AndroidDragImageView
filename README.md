## AndroidDragImageView
重写android里的ImageView控件，能够缩放拖曳图片，支持ViewPager
###使用
DragImageView继承ImageView类，直接在layout文件中引用，但要设置scaleType属性为"matrix"，因为是使用Matrix实现图片的缩放和平移。<\br>
