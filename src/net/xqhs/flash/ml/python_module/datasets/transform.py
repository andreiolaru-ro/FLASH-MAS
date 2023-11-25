from torchvision import transforms

class BaseTransform:
	def transform(self, x):
		pass

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

