from torchvision import transforms

class BaseTransform:
	def transform(self, x):
		pass
	def process_output(self, output):
		return output

class ImageNetTransform(BaseTransform):
	def __init__(self, input_size=[224, 224], mean=[0.485, 0.456, 0.406], std=[0.229, 0.224, 0.225]):
		self.list_of_transforms = []
		self.input_size = input_size
		self.mean = mean
		self.std = std
		self.list_of_transforms.append(transforms.Resize(size=input_size))
		self.list_of_transforms.append(transforms.ToTensor())
		self.list_of_transforms.append(transforms.Normalize(mean, std))
		self.transforms = transforms.Compose(self.list_of_transforms)

	def transform(self, x):
		y = self.transforms(x)
		return y


class CityscapesTransform(BaseTransform):
	def __init__(self, input_size=[1024, 2048], mean=[0.485, 0.456, 0.406], std=[0.229, 0.224, 0.225]):
		self.list_of_transforms = []
		self.input_size = input_size
		self.mean = mean
		self.std = std
		self.list_of_transforms.append(transforms.Resize(size=input_size))
		self.list_of_transforms.append(transforms.ToTensor())
		self.list_of_transforms.append(transforms.Normalize(mean, std))
		self.transforms = transforms.Compose(self.list_of_transforms)

	def transform(self, x):
		y = self.transforms(x)
		return y


class VOCTransform(BaseTransform):
	def __init__(self, input_size=[513, 513], mean=[0.485, 0.456, 0.406], std=[0.229, 0.224, 0.225]):
		self.list_of_transforms = []
		self.input_size = input_size
		self.mean = mean
		self.std = std
		self.list_of_transforms.append(transforms.Resize(size=input_size))
		self.list_of_transforms.append(transforms.ToTensor())
		self.list_of_transforms.append(transforms.Normalize(mean, std))
		self.transforms = transforms.Compose(self.list_of_transforms)

	def transform(self, x):
		y = self.transforms(x)
		return y

class YOLOTransform(BaseTransform):
	def process_output(self, output, classes = "All"):
		from shapely.geometry import Polygon
		i = -1
		nr = {}
		area = {}
		for output in output:
			if output.masks == None:
				continue
			for mask in output.masks:
				lst = []
				i=i+1
				cls = output.boxes[i].cls[0].item()
				if classes != "All" and cls not in classes:
					continue
				for x in mask.xy:
					if cls not in nr: nr[cls] = 0
					nr[cls] += 1
					for y in x:
						lst.append(y)
					polygon = Polygon(lst)
					if cls not in area: area[cls] = 0
					area[cls] += polygon.area	        
		return (nr, area)

class YOLOGetPedestrians(YOLOTransform):
	def process_output(self, output):
		numbers, areas = YOLOTransform.process_output(self, output, classes = [0])
		return (numbers.get(0, 0), areas.get(0, 0))
	
	
	
	